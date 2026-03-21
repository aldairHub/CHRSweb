package org.uteq.backend.service;

import org.uteq.backend.dto.DocPrepostulacionDTO;
import org.uteq.backend.dto.DocumentoResponseDTO;
import org.uteq.backend.dto.PostulanteInfoDTO;
import org.uteq.backend.repository.DocumentoRepositoryCustomImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.uteq.backend.repository.ProcesoEvaluacionRepository;
import org.uteq.backend.repository.FaseProcesoRepository;
import org.uteq.backend.entity.ProcesoEvaluacion;
import org.uteq.backend.entity.FaseProceso;

import java.util.*;

// ============================================================
// DocumentoService
// Lógica de negocio: almacenamiento en Supabase + llamadas a SPs
// ============================================================
@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepositoryCustomImpl documentoRepo;

    @Autowired
    private SupabaseStorageService supabaseService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProcesoEvaluacionRepository procesoRepository;

    @Autowired
    private FaseProcesoRepository faseProcesoRepository;

    // ----------------------------------------------------------
    // Obtener documentos de una postulación (SP 1)
    // ----------------------------------------------------------
    public List<DocumentoResponseDTO> obtenerDocumentosPostulacion(Long idPostulacion) {
        return documentoRepo.obtenerDocumentosPostulacion(idPostulacion);
    }

    // ----------------------------------------------------------
    // Subir archivo a Supabase y registrar en BD (SP 2)
    // ----------------------------------------------------------
    public Map<String, Object> subirDocumento(
            Long idPostulacion,
            Long idTipoDocumento,
            MultipartFile archivo
    ) {
        Map<String, Object> response = new HashMap<>();

        // Validar que el archivo no esté vacío
        if (archivo == null || archivo.isEmpty()) {
            response.put("exitoso", false);
            response.put("mensaje", "El archivo está vacío.");
            return response;
        }

        // Validar tipo (solo PDF)
        String contentType = archivo.getContentType();
        if (!"application/pdf".equals(contentType)) {
            response.put("exitoso", false);
            response.put("mensaje", "Solo se aceptan archivos PDF.");
            return response;
        }

        // Validar tamaño (10 MB)
        if (archivo.getSize() > 10 * 1024 * 1024) {
            response.put("exitoso", false);
            response.put("mensaje", "El archivo supera el tamaño máximo de 10 MB.");
            return response;
        }

        // ✅ VALIDAR que la ventana de subida de docs sigue abierta
        // (puede estar abierta aunque la convocatoria ya haya cerrado)
        try {
            Map<String, Object> infoPostulacion = documentoRepo.obtenerInfoPorPostulacion(idPostulacion);
            Object docsAbiertosObj = infoPostulacion.get("documentos_abiertos");
            if (Boolean.FALSE.equals(docsAbiertosObj)) {
                response.put("exitoso", false);
                response.put("mensaje", "El plazo para subir documentos ha vencido.");
                return response;
            }
        } catch (Exception e) {
            // Si el SP no devuelve ese campo aún, no bloqueamos la subida
            System.out.println("⚠️ No se pudo verificar fecha límite de docs: " + e.getMessage());
        }

        // ✅ SUBIR ARCHIVO A SUPABASE (carpeta "documentos")
        String urlArchivo;
        try {
            System.out.println("📤 Subiendo documento a Supabase...");
            String identificador = "postulacion_" + idPostulacion + "_tipo_" + idTipoDocumento
                    + "_" + UUID.randomUUID();
            urlArchivo = supabaseService.subirArchivo(
                    archivo,
                    "documentos",
                    identificador
            );
            System.out.println("✅ Documento subido exitosamente: " + urlArchivo);
        } catch (Exception e) {
            System.err.println("❌ Error al subir documento a Supabase: " + e.getMessage());
            e.printStackTrace();
            response.put("exitoso", false);
            response.put("mensaje", "Error al subir el archivo: " + e.getMessage());
            return response;
        }

        // Llamar SP 2 — se guarda la URL pública de Supabase en lugar de una ruta local
        Map<String, Object> resultado = documentoRepo.guardarDocumento(
                idPostulacion, idTipoDocumento, urlArchivo
        );

        String mensaje = (String) resultado.get("mensaje");

        if (mensaje != null && mensaje.startsWith("ERROR")) {
            // Si el SP falló, intentar eliminar el archivo ya subido en Supabase
            try {
                supabaseService.eliminarArchivo(urlArchivo);
            } catch (Exception ex) {
                System.err.println("⚠️ No se pudo eliminar el archivo de Supabase tras fallo del SP: " + ex.getMessage());
            }
            response.put("exitoso", false);
            response.put("mensaje", mensaje);
        } else {
            response.put("exitoso", true);
            response.put("mensaje", "Documento subido correctamente.");
            response.put("idDocumento", resultado.get("idDocumento"));
            response.put("rutaArchivo", urlArchivo);
        }

        return response;
    }

    // ----------------------------------------------------------
    // Eliminar un documento (SP 3)
    // ----------------------------------------------------------
    public Map<String, Object> eliminarDocumento(Long idDocumento, Long idPostulacion) {
        return documentoRepo.eliminarDocumento(idDocumento, idPostulacion);
    }

    // ----------------------------------------------------------
    // Finalizar carga (SP 4)
    // ----------------------------------------------------------
    public Map<String, Object> finalizarCarga(Long idPostulacion) {
        return documentoRepo.finalizarCargaDocumentos(idPostulacion);
    }
    // ----------------------------------------------------------
    // Info del postulante (SP 5) — para el header del componente
    // ----------------------------------------------------------
    public PostulanteInfoDTO obtenerInfoPostulante(Long idUsuario) {
        return documentoRepo.obtenerInfoPostulante(idUsuario);
    }

    public List<DocumentoResponseDTO> obtenerDocumentosConvocatoria(Long idPostulacion) {
        return documentoRepo.obtenerDocumentosConvocatoria(idPostulacion);
    }

    public List<DocPrepostulacionDTO> obtenerDocsPrepostulacion(Long idPostulacion) {
        return documentoRepo.obtenerDocsPrepostulacion(idPostulacion);
    }

    public Map<String, Object> validarDocumento(Long idDocumento, String estado, String observacion) {
        Map<String, Object> result = documentoRepo.validarDocumento(idDocumento, estado, observacion);

        // Notificar al postulante según el resultado de la validación
        if ("rechazado".equalsIgnoreCase(estado) || "validado".equalsIgnoreCase(estado)) {
            try {
                Map<String, Object> docInfo = jdbc.queryForMap(
                        "SELECT d.id_postulacion, p.id_usuario, td.nombre AS tipo_doc " +
                                "FROM documento d " +
                                "JOIN postulacion pc ON pc.id_postulacion = d.id_postulacion " +
                                "JOIN postulante p ON p.id_postulante = pc.id_postulante " +
                                "JOIN tipo_documento td ON td.id_tipo_documento = d.id_tipo_documento " +
                                "WHERE d.id_documento = ?", idDocumento
                );
                Long idUsuario = ((Number) docInfo.get("id_usuario")).longValue();
                String tipoDoc = (String) docInfo.getOrDefault("tipo_doc", "documento");

                if ("validado".equalsIgnoreCase(estado)) {
                    notificacionService.notificarUsuario(
                            idUsuario, "success",
                            "Documento validado",
                            "Tu documento '" + tipoDoc + "' ha sido validado correctamente. ¡Sigue subiendo los documentos restantes!",
                            "POSTULACION", null
                    );
                } else {
                    String obs = (observacion != null && !observacion.isBlank()) ? observacion : "Sin observación";
                    notificacionService.notificarUsuario(
                            idUsuario, "warning",
                            "Documento rechazado",
                            "Tu documento '" + tipoDoc + "' fue rechazado. Motivo: " + obs + ". Por favor, súbelo nuevamente.",
                            "POSTULACION", null
                    );
                }
            } catch (Exception ignored) {}
        }

        return result;
    }

    public Map<String, Object> obtenerInfoPorPostulacion(Long idPostulacion) {
        return documentoRepo.obtenerInfoPorPostulacion(idPostulacion);
    }

    public Map<String, Object> enviarARevision(Long idPostulacion, Long idUsuarioPostulante) {
        try {
            // Obtener nombre del postulante para la notificación
            String nombrePostulante = "";
            try {
                Map<String, Object> info = jdbc.queryForMap(
                        "SELECT pt.nombres_postulante || ' ' || pt.apellidos_postulante AS nombre " +
                                "FROM postulante pt " +
                                "JOIN postulacion po ON po.id_postulante = pt.id_postulante " +
                                "WHERE po.id_postulacion = ?", idPostulacion
                );
                nombrePostulante = (String) info.get("nombre");
            } catch (Exception ignored) {}

            // Notificar a todos los evaluadores
            notificacionService.notificarRol(
                    "evaluador", "info",
                    "Documentos listos para revisión",
                    "El postulante " + nombrePostulante + " ha enviado sus documentos y están listos para revisión.",
                    "POSTULACION", idPostulacion
            );

            return Map.of("exitoso", true, "mensaje", "Evaluadores notificados correctamente.");

        } catch (Exception e) {
            return Map.of("exitoso", false, "mensaje", "Error al notificar: " + e.getMessage());
        }
    }

    public Map<String, Object> obtenerResultadosPostulante(Long idUsuario) {
        return documentoRepo.obtenerResultadosPostulanteConFiltro(idUsuario, null);
    }

    public Map<String, Object> obtenerResultadosPostulanteConFiltro(Long idUsuario, Long idPostulacion) {
        return documentoRepo.obtenerResultadosPostulanteConFiltro(idUsuario, idPostulacion);
    }

    // ── MÉTODO 1: Listar todas las postulaciones de un usuario ─────────────────
    /**
     * Devuelve todas las postulaciones activas del usuario.
     * Delega al SP existente o usa una nueva query.
     */
    public List<PostulanteInfoDTO> listarPostulacionesUsuario(Long idUsuario) {
        String sql = """
        SELECT
            p.id_postulante,
            p.nombres_postulante    AS nombres,
            p.apellidos_postulante  AS apellidos,
            p.identificacion,
            p.correo_postulante     AS correo,
            pc.id_postulacion,
            pc.estado_postulacion,
            m.nombre_materia,
            ca.nombre_carrera,
            ar.nombre_area,
            false                   AS documentos_abiertos,
            NULL                    AS fecha_limite_documentos,
            c.titulo                AS nombre_convocatoria,
            c.id_convocatoria
        FROM postulacion pc
        JOIN postulante p   ON p.id_postulante  = pc.id_postulante
        JOIN usuario u      ON u.id_usuario     = p.id_usuario
        JOIN solicitud_docente sd ON sd.id_solicitud = pc.id_solicitud
        LEFT JOIN materia m   ON m.id_materia  = sd.id_materia
        LEFT JOIN carrera ca  ON ca.id_carrera = sd.id_carrera
        LEFT JOIN area_conocimiento ar ON ar.id_area = sd.id_area
        JOIN convocatoria_solicitud cs ON cs.id_solicitud = sd.id_solicitud
        JOIN convocatoria c ON c.id_convocatoria = cs.id_convocatoria
        WHERE p.id_usuario = ?
        ORDER BY pc.fecha DESC
        """;

        return jdbc.query(sql, new Object[]{idUsuario}, (rs, rowNum) -> {
            PostulanteInfoDTO dto = new PostulanteInfoDTO();
            dto.setIdPostulante(rs.getLong("id_postulante"));
            dto.setNombres(rs.getString("nombres"));
            dto.setApellidos(rs.getString("apellidos"));
            dto.setIdentificacion(rs.getString("identificacion"));
            dto.setCorreo(rs.getString("correo"));
            dto.setIdPostulacion(rs.getLong("id_postulacion"));
            dto.setEstadoPostulacion(rs.getString("estado_postulacion"));
            dto.setNombreMateria(rs.getString("nombre_materia"));
            dto.setNombreCarrera(rs.getString("nombre_carrera"));
            dto.setNombreArea(rs.getString("nombre_area"));
            dto.setDocumentosAbiertos(false);
            dto.setFechaLimiteDocumentos(null);
            dto.setNombreConvocatoria(rs.getString("nombre_convocatoria"));
            long idConv = rs.getLong("id_convocatoria");
            dto.setIdConvocatoria(rs.wasNull() ? null : idConv);
            return dto;
        });
    }

    // ── MÉTODO 2: Info de postulante para una postulación específica ───────────
    public PostulanteInfoDTO obtenerInfoPostulantePorPostulacion(Long idUsuario, Long idPostulacion) {
        // Reutiliza el SP existente pero valida que la postulación pertenece al usuario
        PostulanteInfoDTO info = documentoRepo.obtenerInfoPostulante(idUsuario);
        if (info != null && info.getIdPostulacion().equals(idPostulacion)) {
            return info;
        }
        // Si el SP devuelve otra postulación, busca directamente la pedida
        String sql = """
        SELECT * FROM sp_obtener_info_postulante_por_postulacion(?, ?)
        """;
        // Si el SP no existe aún, usa query directa:
        String sqlDirecto = """
        SELECT
            p.id_postulante,
            u.nombres,
            u.apellidos,
            u.identificacion,
            u.correo,
            pc.id_postulacion,
            pc.estado_postulacion,
            m.nombre_materia,
            ca.nombre_carrera,
            ar.nombre_area,
            false AS documentos_abiertos,
            NULL  AS fecha_limite_documentos
        FROM postulacion pc
        JOIN postulante p  ON p.id_postulante  = pc.id_postulante
        JOIN usuario u     ON u.id_usuario     = p.id_usuario
        JOIN solicitud_docente sd ON sd.id_solicitud = pc.id_solicitud
        LEFT JOIN materia m   ON m.id_materia   = sd.id_materia
        LEFT JOIN carrera ca  ON ca.id_carrera  = sd.id_carrera
        LEFT JOIN area_conocimiento ar ON ar.id_area = sd.id_area
        WHERE p.id_usuario = ? AND pc.id_postulacion = ?
        """;
        List<PostulanteInfoDTO> list = jdbc.query(sqlDirecto,
                new Object[]{idUsuario, idPostulacion}, (rs, rowNum) -> {
                    PostulanteInfoDTO dto = new PostulanteInfoDTO();
                    dto.setIdPostulante(rs.getLong("id_postulante"));
                    dto.setNombres(rs.getString("nombres"));
                    dto.setApellidos(rs.getString("apellidos"));
                    dto.setIdentificacion(rs.getString("identificacion"));
                    dto.setCorreo(rs.getString("correo"));
                    dto.setIdPostulacion(rs.getLong("id_postulacion"));
                    dto.setEstadoPostulacion(rs.getString("estado_postulacion"));
                    dto.setNombreMateria(rs.getString("nombre_materia"));
                    dto.setNombreCarrera(rs.getString("nombre_carrera"));
                    dto.setNombreArea(rs.getString("nombre_area"));
                    return dto;
                });
        return list.isEmpty() ? null : list.get(0);
    }

    // ── MÉTODO 3: Progreso del proceso de evaluación (para tiempo real) ────────
    public Map<String, Object> obtenerProgresoPostulante(Long idUsuario, Long idPostulacion) {

        // ── Buscar el proceso: si idPostulacion es null se toma el más reciente ──
        // Se evita pasar null en Object[] para no generar error de tipo en PostgreSQL.
        final List<Map<String, Object>> rows;
        if (idPostulacion != null) {
            String sql = """
            SELECT pe.*
            FROM proceso_evaluacion pe
            JOIN postulante p ON p.id_postulante = pe.id_postulante
            WHERE p.id_usuario = ?
              AND pe.id_solicitud = (
                  SELECT id_solicitud FROM postulacion WHERE id_postulacion = ?
              )
            ORDER BY pe.fecha_inicio DESC
            LIMIT 1
            """;
            rows = jdbc.queryForList(sql, new Object[]{idUsuario, idPostulacion});
        } else {
            String sql = """
            SELECT pe.*
            FROM proceso_evaluacion pe
            JOIN postulante p ON p.id_postulante = pe.id_postulante
            WHERE p.id_usuario = ?
            ORDER BY pe.fecha_inicio DESC
            LIMIT 1
            """;
            rows = jdbc.queryForList(sql, new Object[]{idUsuario});
        }

        if (rows.isEmpty()) return null;

        Map<String, Object> procesoRow = rows.get(0);
        Long idProceso = ((Number) procesoRow.get("id_proceso")).longValue();

        // ── Fases del proceso: incluye tipo para que el frontend pueda filtrar ──
        String sqlFases = """
        SELECT fp.id_fase_proceso,
               f.nombre,
               f.tipo,
               f.orden,
               f.peso,
               fp.estado,
               -- Solo devolver calificacion si la fase está completada u omitida.
               -- Si está pendiente/bloqueada/en_curso la calificacion residual se ignora.
               CASE WHEN fp.estado IN ('completada', 'omitida')
                    THEN fp.calificacion
                    ELSE NULL
               END AS calificacion,
               to_char(fp.fecha_completada, 'DD/MM/YYYY') AS fecha_completada
        FROM fase_proceso fp
        JOIN fase_evaluacion f ON f.id_fase = fp.id_fase
        WHERE fp.id_proceso = ?
        ORDER BY f.orden ASC
        """;
        List<Map<String, Object>> fases = jdbc.queryForList(sqlFases, new Object[]{idProceso});

        // ── Calcular puntajes en tiempo real desde las fases, no desde las columnas ──
        // Las columnas puntaje_matriz/entrevista/final pueden tener datos residuales
        // si el proceso fue reiniciado sin limpiar esas columnas.
        double puntajeMatrizReal     = 0;
        double puntajeEntrevistaReal = 0;
        for (Map<String, Object> fase : fases) {
            String estado = (String) fase.get("estado");
            if (!"completada".equals(estado) && !"omitida".equals(estado)) continue;
            Object cal = fase.get("calificacion");
            if (cal == null) continue;
            double valor = ((Number) cal).doubleValue();
            String tipo = (String) fase.get("tipo");
            if ("entrevista".equals(tipo)) {
                puntajeEntrevistaReal += valor;
            } else {
                puntajeMatrizReal += valor;
            }
        }
        double puntajeFinalReal = puntajeMatrizReal + puntajeEntrevistaReal;

        // Solo usar puntaje_matriz de la columna si ninguna fase tiene calificacion real
        // (caso: la matriz de méritos se guarda por separado, no como fases)
        Object colPuntajeMatriz = procesoRow.get("puntaje_matriz");
        double colMatrizVal = colPuntajeMatriz != null ? ((Number) colPuntajeMatriz).doubleValue() : 0;
        if (puntajeMatrizReal == 0 && colMatrizVal > 0) {
            // La matriz de méritos se cargó externamente — verificar que el proceso no esté reiniciado
            // comprobando si alguna fase está completada. Si no hay ninguna completada, no mostrar.
            boolean hayFaseCompletada = fases.stream()
                    .anyMatch(f -> "completada".equals(f.get("estado")));
            if (hayFaseCompletada) {
                puntajeMatrizReal = colMatrizVal;
                puntajeFinalReal  = puntajeMatrizReal + puntajeEntrevistaReal;
            }
        }

        // Progreso: recalcular desde fases para evitar datos residuales
        long totalFases     = fases.size();
        long fasesCompletadas = fases.stream()
                .filter(f -> "completada".equals(f.get("estado")) || "omitida".equals(f.get("estado")))
                .count();
        int progresoReal = totalFases > 0 ? (int) Math.round((double) fasesCompletadas / totalFases * 100) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("idProceso",         idProceso);
        result.put("estadoGeneral",     procesoRow.get("estado_general"));
        result.put("faseActual",        procesoRow.get("fase_actual"));
        result.put("progreso",          progresoReal);
        result.put("puntajeMatriz",     puntajeMatrizReal > 0 ? puntajeMatrizReal : null);
        result.put("puntajeEntrevista", puntajeEntrevistaReal > 0 ? puntajeEntrevistaReal : null);
        result.put("puntajeFinal",      puntajeFinalReal > 0 ? puntajeFinalReal : null);
        result.put("decision",          procesoRow.get("decision"));
        result.put("justificacion",     procesoRow.get("justificacion_decision"));
        result.put("fases",             fases);
        return result;
    }

}