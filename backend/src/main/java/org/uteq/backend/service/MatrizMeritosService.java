package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.repository.PostgresProcedureRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatrizMeritosService {

    private final JdbcTemplate jdbc;
    private final PostgresProcedureRepository procedureRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── Obtener matriz completa de una convocatoria ───────────
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMatriz(Long idConvocatoria) {

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

        List<Map<String, Object>> candidatos = new ArrayList<>();
        for (Map<String, Object> raw : candidatosRaw) {
            Long idProceso = ((Number) raw.get("id_proceso")).longValue();

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

    // ─── Guardar puntajes usando stored procedure ──────────────
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> guardarPuntajes(Map<String, Object> payload) {
        List<Map<String, Object>> candidatos = (List<Map<String, Object>>) payload.get("candidatos");

        for (Map<String, Object> c : candidatos) {
            Long idProceso = ((Number) c.get("idProceso")).longValue();
            Map<String, Object> puntajes = (Map<String, Object>) c.get("puntajes");
            Map<String, Object> accionesAfirmativas = (Map<String, Object>) c.get("accionesAfirmativas");
            Object puntajeTotal = c.get("puntajeTotal");

            List<String> items = new ArrayList<>();
            List<String> valores = new ArrayList<>();

            for (Map.Entry<String, Object> entry : puntajes.entrySet()) {
                items.add(entry.getKey());
                valores.add(String.valueOf(entry.getValue()));
            }

            for (Map.Entry<String, Object> entry : accionesAfirmativas.entrySet()) {
                items.add(entry.getKey());
                valores.add(String.valueOf(entry.getValue()));
            }

            double total = puntajeTotal != null ? ((Number) puntajeTotal).doubleValue() : 0.0;

            procedureRepo.guardarMatrizMeritos(idProceso, items, valores, total);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("mensaje", "Puntajes guardados correctamente");
        result.put("total", candidatos.size());
        return result;
    }

    // ─── Listar convocatorias para la lista de matriz ──────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarConvocatorias() {

        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT
                c.id_convocatoria,
                c.titulo,
                c.fecha_limite_documentos,
                cs.id_solicitud,
                m.nombre_materia,
                COUNT(DISTINCT pe.id_proceso) AS total_candidatos
            FROM convocatoria c
            JOIN convocatoria_solicitud cs ON c.id_convocatoria = cs.id_convocatoria
            JOIN solicitud_docente sd ON cs.id_solicitud = sd.id_solicitud
            LEFT JOIN materia m ON sd.id_materia = m.id_materia
            LEFT JOIN proceso_evaluacion pe ON pe.id_solicitud = cs.id_solicitud
            GROUP BY c.id_convocatoria, c.titulo, c.fecha_limite_documentos,
                     cs.id_solicitud, m.nombre_materia
            ORDER BY c.fecha_limite_documentos DESC NULLS LAST, c.id_convocatoria, cs.id_solicitud
            """);

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (Map<String, Object> row : rows) {
            boolean disponible = false;
            String mensajeBloqueo = null;
            String fechaLimiteStr = null;
            long totalCandidatos = ((Number) row.get("total_candidatos")).longValue();

            Object fechaObj = row.get("fecha_limite_documentos");
            if (fechaObj != null) {
                LocalDate fechaLimite = ((java.sql.Date) fechaObj).toLocalDate();
                fechaLimiteStr = fechaLimite.format(DATE_FMT);
                if (hoy.isBefore(fechaLimite)) {
                    mensajeBloqueo = "Disponible a partir del " + fechaLimiteStr;
                } else if (totalCandidatos == 0) {
                    mensajeBloqueo = "No hay candidatos con proceso activo.";
                } else {
                    disponible = true;
                }
            } else {
                if (totalCandidatos == 0) {
                    mensajeBloqueo = "No hay candidatos con proceso activo.";
                } else {
                    mensajeBloqueo = "Sin fecha límite definida.";
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("idConvocatoria",        row.get("id_convocatoria"));
            item.put("titulo",                row.get("titulo"));
            item.put("fechaLimiteDocumentos", fechaLimiteStr);
            item.put("idSolicitud",           row.get("id_solicitud"));
            item.put("materia",               row.get("nombre_materia"));
            item.put("totalCandidatos",       totalCandidatos);
            item.put("disponible",            disponible);
            item.put("mensajeBloqueo",        mensajeBloqueo);
            result.add(item);
        }
        return result;
    }

    // ─── Habilitar entrevista manualmente ──────────────────────
    public void habilitarEntrevista(Long idProceso, String justificacion) {
        procedureRepo.habilitarEntrevista(idProceso, justificacion);
    }

    // ─── Obtener matriz por solicitud ──────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMatrizPorSolicitud(Long idSolicitud) {

        // 1. Info de la solicitud/convocatoria
        Map<String, Object> conv = jdbc.queryForMap("""
            SELECT c.id_convocatoria, c.titulo, c.fecha_limite_documentos,
                   m.nombre_materia, cs.id_solicitud
            FROM convocatoria_solicitud cs
            JOIN convocatoria c ON cs.id_convocatoria = c.id_convocatoria
            JOIN solicitud_docente sd ON cs.id_solicitud = sd.id_solicitud
            LEFT JOIN materia m ON sd.id_materia = m.id_materia
            WHERE cs.id_solicitud = ?
            LIMIT 1
            """, idSolicitud);

        // 2. Verificar bloqueo por fecha
        boolean bloqueada = false;
        String mensajeBloqueo = null;
        String fechaLimiteStr = null;
        Object fechaLimiteObj = conv.get("fecha_limite_documentos");

        if (fechaLimiteObj != null) {
            LocalDate fechaLimite = ((java.sql.Date) fechaLimiteObj).toLocalDate();
            fechaLimiteStr = fechaLimite.format(DATE_FMT);
            if (LocalDate.now().isBefore(fechaLimite)) {
                bloqueada = true;
                mensajeBloqueo = "La matriz estará disponible a partir del " + fechaLimiteStr
                        + ". El período de subida de documentos aún está abierto.";
            }
        }

        // 2b. Verificar bloqueo por documentos obligatorios no validados
        if (!bloqueada) {
            Integer obligatoriosSinValidar = jdbc.queryForObject(
                    "select fn_documentos_pendientes_solicitud(?)",
                    Integer.class, idSolicitud);

            if (obligatoriosSinValidar != null && obligatoriosSinValidar > 0) {
                bloqueada = true;
                mensajeBloqueo = "La matriz está bloqueada. Existen " + obligatoriosSinValidar
                        + " documento(s) obligatorio(s) pendientes de validación.";
            }
        }

        Map<String, Object> convocatoriaInfo = new HashMap<>();
        convocatoriaInfo.put("idConvocatoria", conv.get("id_convocatoria"));
        convocatoriaInfo.put("idSolicitud", idSolicitud);
        convocatoriaInfo.put("titulo", conv.get("titulo"));
        convocatoriaInfo.put("materia", conv.get("nombre_materia"));
        convocatoriaInfo.put("fechaLimiteDocumentos", fechaLimiteStr);
        convocatoriaInfo.put("bloqueada", bloqueada);
        convocatoriaInfo.put("mensajeBloqueo", mensajeBloqueo);

        // 3. Candidatos por id_solicitud
        List<Map<String, Object>> candidatosRaw = jdbc.queryForList("""
            SELECT
                p.id_postulante,
                pe.id_proceso,
                pe.id_solicitud,
                p.nombres_postulante AS nombres,
                p.apellidos_postulante AS apellidos,
                pre.nombres AS titulos,
                pe.habilitado_entrevista
            FROM proceso_evaluacion pe
            JOIN postulante p ON pe.id_postulante = p.id_postulante
            JOIN prepostulacion pre ON p.id_prepostulacion = pre.id_prepostulacion
            WHERE pe.id_solicitud = ?
            ORDER BY p.apellidos_postulante
            """, idSolicitud);

        // 4. Cargar puntajes guardados
        List<Map<String, Object>> candidatos = new ArrayList<>();
        for (Map<String, Object> raw : candidatosRaw) {
            Long idProceso = ((Number) raw.get("id_proceso")).longValue();

            Map<String, Object> puntajes = new HashMap<>();
            Map<String, Object> accionesAfirmativas = new HashMap<>();

            List<Map<String, Object>> puntajesGuardados = jdbc.queryForList(
                    "SELECT item_id, valor FROM matriz_meritos_puntaje WHERE id_proceso = ?", idProceso);

            for (Map<String, Object> p : puntajesGuardados) {
                String itemId = (String) p.get("item_id");
                Object valor = p.get("valor");
                if (itemId != null && itemId.startsWith("af_")) {
                    accionesAfirmativas.put(itemId, "true".equals(String.valueOf(valor)));
                } else {
                    puntajes.put(itemId, valor);
                }
            }

            Map<String, Object> candidato = new HashMap<>();
            candidato.put("idPostulante", raw.get("id_postulante"));
            candidato.put("idProceso", idProceso);
            candidato.put("idSolicitud", idSolicitud);
            candidato.put("nombres", raw.get("nombres"));
            candidato.put("apellidos", raw.get("apellidos"));
            candidato.put("titulos", raw.get("titulos"));
            candidato.put("puntajes", puntajes);
            candidato.put("accionesAfirmativas", accionesAfirmativas);
            candidato.put("habilitadoEntrevista", Boolean.TRUE.equals(raw.get("habilitado_entrevista")));
            candidatos.add(candidato);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("convocatoria", convocatoriaInfo);
        result.put("candidatos", candidatos);
        return result;
    }
}