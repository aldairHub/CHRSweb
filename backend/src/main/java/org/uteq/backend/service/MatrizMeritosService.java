package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatrizMeritosService {

    private final JdbcTemplate jdbc;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── Obtener matriz completa de una convocatoria ───────────
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMatriz(Long idConvocatoria) {

        // 1. Info de la convocatoria
        Map<String, Object> conv = jdbc.queryForMap("""
                SELECT c.id_convocatoria, c.titulo, c.fecha_limite_documentos,
                       m.nombre_materia
                FROM convocatoria c
                LEFT JOIN convocatoria_solicitud cs ON c.id_convocatoria = cs.id_convocatoria
                LEFT JOIN solicitud_docente sd ON cs.id_solicitud = sd.id_solicitud
                LEFT JOIN materia m ON sd.id_materia = m.id_materia
                WHERE c.id_convocatoria = ?
                LIMIT 1
                """, idConvocatoria);

        // 2. Verificar si está bloqueada (fecha_limite_documentos > hoy)
        boolean bloqueada = false;
        String mensajeBloqueo = null;
        Object fechaLimiteObj = conv.get("fecha_limite_documentos");
        String fechaLimiteStr = null;

        if (fechaLimiteObj != null) {
            LocalDate fechaLimite = ((java.sql.Date) fechaLimiteObj).toLocalDate();
            fechaLimiteStr = fechaLimite.format(DATE_FMT);
            if (LocalDate.now().isBefore(fechaLimite)) {
                bloqueada = true;
                mensajeBloqueo = "La matriz estará disponible a partir del " + fechaLimiteStr
                        + ". El período de subida de documentos aún está abierto.";
            }
        }

        Map<String, Object> convocatoriaInfo = new HashMap<>();
        convocatoriaInfo.put("idConvocatoria", idConvocatoria);
        convocatoriaInfo.put("titulo", conv.get("titulo"));
        convocatoriaInfo.put("materia", conv.get("nombre_materia"));
        convocatoriaInfo.put("fechaLimiteDocumentos", fechaLimiteStr);
        convocatoriaInfo.put("bloqueada", bloqueada);
        convocatoriaInfo.put("mensajeBloqueo", mensajeBloqueo);

        // 3. Candidatos (postulantes con proceso activo en esta convocatoria)
        List<Map<String, Object>> candidatosRaw = jdbc.queryForList("""
        SELECT
            p.id_postulante,
            pe.id_proceso,
            pe.id_solicitud,
            p.nombres_postulante AS nombres,
            p.apellidos_postulante AS apellidos,
            pre.nombres AS titulos
        FROM proceso_evaluacion pe
        JOIN postulante p ON pe.id_postulante = p.id_postulante
        JOIN prepostulacion pre ON p.id_prepostulacion = pre.id_prepostulacion
        JOIN convocatoria_solicitud cs ON pe.id_solicitud = cs.id_solicitud
        WHERE cs.id_convocatoria = ?
        ORDER BY p.apellidos_postulante
        """, idConvocatoria);

        // 4. Para cada candidato, cargar puntajes guardados si existen
        List<Map<String, Object>> candidatos = new ArrayList<>();
        for (Map<String, Object> raw : candidatosRaw) {
            Long idProceso = ((Number) raw.get("id_proceso")).longValue();

            // Puntajes guardados
            Map<String, Object> puntajes = new HashMap<>();
            Map<String, Object> accionesAfirmativas = new HashMap<>();

            List<Map<String, Object>> puntajesGuardados = jdbc.queryForList("""
                    SELECT item_id, valor
                    FROM matriz_meritos_puntaje
                    WHERE id_proceso = ?
                    """, idProceso);

            for (Map<String, Object> p : puntajesGuardados) {
                String itemId = (String) p.get("item_id");
                Object valor = p.get("valor");
                if (itemId != null && itemId.startsWith("af_")) {
                    accionesAfirmativas.put(itemId, Boolean.TRUE.equals(valor) || "true".equals(String.valueOf(valor)));
                } else {
                    puntajes.put(itemId, valor);
                }
            }

            Map<String, Object> candidato = new HashMap<>();
            candidato.put("idPostulante", raw.get("id_postulante"));
            candidato.put("idProceso", idProceso);
            candidato.put("idSolicitud", raw.get("id_solicitud"));
            candidato.put("nombres", raw.get("nombres"));
            candidato.put("apellidos", raw.get("apellidos"));
            candidato.put("titulos", raw.get("titulos"));
            candidato.put("puntajes", puntajes);
            candidato.put("accionesAfirmativas", accionesAfirmativas);
            candidatos.add(candidato);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("convocatoria", convocatoriaInfo);
        result.put("candidatos", candidatos);
        return result;
    }

    // ─── Guardar puntajes ──────────────────────────────────────
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> guardarPuntajes(Map<String, Object> payload) {
        List<Map<String, Object>> candidatos = (List<Map<String, Object>>) payload.get("candidatos");

        for (Map<String, Object> c : candidatos) {
            Long idProceso = ((Number) c.get("idProceso")).longValue();
            Map<String, Object> puntajes = (Map<String, Object>) c.get("puntajes");
            Map<String, Object> accionesAfirmativas = (Map<String, Object>) c.get("accionesAfirmativas");

            // Eliminar puntajes anteriores
            jdbc.update("DELETE FROM matriz_meritos_puntaje WHERE id_proceso = ?", idProceso);

            // Insertar puntajes de ítems
            for (Map.Entry<String, Object> entry : puntajes.entrySet()) {
                jdbc.update("""
                        INSERT INTO matriz_meritos_puntaje (id_proceso, item_id, valor)
                        VALUES (?, ?, ?)
                        """, idProceso, entry.getKey(), String.valueOf(entry.getValue()));
            }

            // Insertar acciones afirmativas
            for (Map.Entry<String, Object> entry : accionesAfirmativas.entrySet()) {
                jdbc.update("""
                        INSERT INTO matriz_meritos_puntaje (id_proceso, item_id, valor)
                        VALUES (?, ?, ?)
                        """, idProceso, entry.getKey(), String.valueOf(entry.getValue()));
            }

            // Actualizar puntaje total en proceso_evaluacion
            Object puntajeTotal = c.get("puntajeTotal");
            if (puntajeTotal != null) {
                jdbc.update("""
                        UPDATE proceso_evaluacion
                        SET puntaje_matriz = ?
                        WHERE id_proceso = ?
                        """, ((Number) puntajeTotal).doubleValue(), idProceso);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("mensaje", "Puntajes guardados correctamente");
        result.put("total", candidatos.size());
        return result;
    }
    // ─── AGREGAR este método en MatrizMeritosService.java ───────────────────────
// Dentro de la clase MatrizMeritosService, después del método obtenerMatriz()

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarConvocatorias() {

        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT
                c.id_convocatoria,
                c.titulo,
                c.fecha_limite_documentos,
                m.nombre_materia,
                COUNT(DISTINCT pe.id_proceso) AS total_candidatos
            FROM convocatoria c
            JOIN convocatoria_solicitud cs ON c.id_convocatoria = cs.id_convocatoria
            JOIN solicitud_docente sd ON cs.id_solicitud = sd.id_solicitud
            LEFT JOIN materia m ON sd.id_materia = m.id_materia
            JOIN proceso_evaluacion pe ON pe.id_solicitud = cs.id_solicitud
            GROUP BY c.id_convocatoria, c.titulo, c.fecha_limite_documentos, m.nombre_materia
            ORDER BY c.fecha_limite_documentos DESC
            """);

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (Map<String, Object> row : rows) {
            boolean disponible = false;
            Long diasRestantes = null;
            String fechaLimiteStr = null;

            Object fechaObj = row.get("fecha_limite_documentos");
            if (fechaObj != null) {
                LocalDate fechaLimite = ((java.sql.Date) fechaObj).toLocalDate();
                fechaLimiteStr = fechaLimite.format(DATE_FMT);
                disponible = !hoy.isBefore(fechaLimite); // disponible cuando fecha_limite <= hoy
                if (hoy.isBefore(fechaLimite)) {
                    diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaLimite);
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("idConvocatoria", row.get("id_convocatoria"));
            item.put("titulo", row.get("titulo"));
            item.put("materia", row.get("nombre_materia"));
            item.put("fechaLimiteDocumentos", fechaLimiteStr);
            item.put("totalCandidatos", ((Number) row.get("total_candidatos")).longValue());
            item.put("disponible", disponible);
            item.put("diasRestantes", diasRestantes);
            result.add(item);
        }
        return result;
    }
}
