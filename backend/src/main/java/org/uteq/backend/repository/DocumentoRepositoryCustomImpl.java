package org.uteq.backend.repository;

import org.uteq.backend.dto.DocPrepostulacionDTO;
import  org.uteq.backend.dto.DocumentoResponseDTO;
import org.uteq.backend.dto.PostulanteInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// DocumentoRepositoryCustomImpl
// Implementación que llama a los SPs de PostgreSQL
// usando SimpleJdbcCall (OUT params) y JdbcTemplate (RETURNS TABLE)
// ============================================================
@Repository
public class DocumentoRepositoryCustomImpl {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DocumentoRepositoryCustomImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ----------------------------------------------------------
    // SP 1: sp_obtener_documentos_postulacion
    // RETURNS TABLE → usamos JdbcTemplate con query nativa
    // ----------------------------------------------------------
    public List<DocumentoResponseDTO> obtenerDocumentosPostulacion(Long idPostulacion) {
        String sql = "SELECT * FROM sp_obtener_documentos_postulacion(?)";

        return jdbcTemplate.query(sql, new Object[]{idPostulacion}, (rs, rowNum) -> {
            DocumentoResponseDTO dto = new DocumentoResponseDTO();
            dto.setIdTipoDocumento(rs.getLong("id_tipo_documento"));
            dto.setNombreTipo(rs.getString("nombre_tipo"));
            dto.setObligatorio(rs.getBoolean("obligatorio"));

            long idDoc = rs.getLong("id_documento");
            dto.setIdDocumento(rs.wasNull() ? null : idDoc);
            dto.setEstadoValidacion(rs.getString("estado_validacion"));
            dto.setRutaArchivo(rs.getString("ruta_archivo"));

            if (rs.getTimestamp("fecha_carga") != null) {
                dto.setFechaCarga(rs.getTimestamp("fecha_carga").toLocalDateTime().toString());
            }
            dto.setObservacionesIa(rs.getString("observaciones_ia"));
            return dto;
        });
    }

    // ----------------------------------------------------------
    // SP 2: sp_guardar_documento
    // OUT params → usamos SimpleJdbcCall
    // ----------------------------------------------------------
    public Map<String, Object> guardarDocumento(Long idPostulacion, Long idTipoDocumento, String rutaArchivo) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withFunctionName("sp_guardar_documento");  // PostgreSQL trata SPs con OUT como functions

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_id_postulacion", idPostulacion)
                .addValue("p_id_tipo_documento", idTipoDocumento)
                .addValue("p_ruta_archivo", rutaArchivo);

        // Para PostgreSQL con OUT params usamos JDBC directo
        Map<String, Object> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT v_id_documento, v_mensaje FROM sp_guardar_documento(?, ?, ?)",
                new Object[]{idPostulacion, idTipoDocumento, rutaArchivo},
                rs -> {
                    result.put("idDocumento", rs.getLong("v_id_documento"));
                    result.put("mensaje", rs.getString("v_mensaje"));
                }
        );
        return result;
    }

    // ----------------------------------------------------------
    // SP 3: sp_eliminar_documento
    // ----------------------------------------------------------
    public Map<String, Object> eliminarDocumento(Long idDocumento, Long idPostulacion) {
        Map<String, Object> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT v_eliminado, v_mensaje FROM sp_eliminar_documento(?, ?)",
                new Object[]{idDocumento, idPostulacion},
                rs -> {
                    result.put("eliminado", rs.getBoolean("v_eliminado"));
                    result.put("mensaje", rs.getString("v_mensaje"));
                }
        );
        return result;
    }

    // ----------------------------------------------------------
    // SP 4: sp_finalizar_carga_documentos
    // ----------------------------------------------------------
    public Map<String, Object> finalizarCargaDocumentos(Long idPostulacion) {
        Map<String, Object> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT * FROM sp_finalizar_carga_documentos(?)",
                new Object[]{idPostulacion},
                rs -> {
                    result.put("exitoso", rs.getBoolean("exitoso"));
                    result.put("mensaje", rs.getString("mensaje"));
                }
        );
        return result;
    }

    // ----------------------------------------------------------
    // SP 5: sp_obtener_info_postulante
    // ----------------------------------------------------------
    public PostulanteInfoDTO obtenerInfoPostulante(Long idUsuario) {
        String sql = "SELECT * FROM sp_obtener_info_postulante(?)";

        List<PostulanteInfoDTO> list = jdbcTemplate.query(sql, new Object[]{idUsuario}, (rs, rowNum) -> {
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
            // Campos de ventana de docs (el SP devuelve estos si existen)
            try {
                dto.setFechaLimiteDocumentos(rs.getString("fecha_limite_documentos"));
                dto.setDocumentosAbiertos(rs.getBoolean("documentos_abiertos"));
            } catch (Exception ignored) {
                // Si el SP aún no devuelve estos campos, no falla
            }
            return dto;
        });

        return list.isEmpty() ? null : list.get(0);
    }

    // SP: sp_obtener_documentos_convocatoria
    public List<DocumentoResponseDTO> obtenerDocumentosConvocatoria(Long idPostulacion) {
        String sql = "SELECT * FROM sp_obtener_documentos_convocatoria(?)";
        return jdbcTemplate.query(sql, new Object[]{idPostulacion}, (rs, rowNum) -> {
            DocumentoResponseDTO dto = new DocumentoResponseDTO();
            dto.setIdTipoDocumento(rs.getLong("id_tipo_documento"));
            dto.setNombreTipo(rs.getString("nombre_tipo"));
            dto.setDescripcionTipo(rs.getString("descripcion_tipo"));
            dto.setObligatorio(rs.getBoolean("obligatorio"));
            long idDoc = rs.getLong("id_documento");
            dto.setIdDocumento(rs.wasNull() ? null : idDoc);
            dto.setEstadoValidacion(rs.getString("estado_validacion"));
            dto.setRutaArchivo(rs.getString("ruta_archivo"));
            if (rs.getTimestamp("fecha_carga") != null)
                dto.setFechaCarga(rs.getTimestamp("fecha_carga").toLocalDateTime().toString());
            dto.setObservacionesIa(rs.getString("observaciones_ia"));
            return dto;
        });
    }

    // SP: sp_obtener_docs_prepostulacion
    public List<DocPrepostulacionDTO> obtenerDocsPrepostulacion(Long idPostulacion) {
        String sql = "SELECT * FROM sp_obtener_docs_prepostulacion(?)";
        return jdbcTemplate.query(sql, new Object[]{idPostulacion}, (rs, rowNum) -> {
            DocPrepostulacionDTO dto = new DocPrepostulacionDTO();
            dto.setIdDocumento(rs.getLong("id_documento"));
            dto.setDescripcion(rs.getString("descripcion"));
            dto.setUrlDocumento(rs.getString("url_documento"));
            if (rs.getTimestamp("fecha_subida") != null)
                dto.setFechaSubida(rs.getTimestamp("fecha_subida").toInstant().toString());
            return dto;
        });
    }

    // ----------------------------------------------------------
    // Actualiza estado_validacion de un documento
    // Guarda observación en resultados_ia_documento si se provee
    // ----------------------------------------------------------
    public Map<String, Object> validarDocumento(Long idDocumento, String estado, String observacion) {
        try {
            jdbcTemplate.update(
                    "UPDATE documento SET estado_validacion = ? WHERE id_documento = ?",
                    estado, idDocumento
            );
            if (observacion != null && !observacion.isBlank()) {
                jdbcTemplate.update(
                        "INSERT INTO resultados_ia_documento (id_documento, resultado, observaciones, fecha_revision) " +
                                "VALUES (?, 'REVISION_MANUAL', ?, NOW())",
                        idDocumento, observacion
                );
            }
            return Map.of("exitoso", true, "mensaje", "Documento actualizado correctamente");
        } catch (Exception e) {
            return Map.of("exitoso", false, "mensaje", "Error al actualizar: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // Info básica del postulante a partir del id_postulacion
    // ----------------------------------------------------------
    public Map<String, Object> obtenerInfoPorPostulacion(Long idPostulacion) {
        String sql =
                "SELECT p.nombres_postulante AS nombres, p.apellidos_postulante AS apellidos, p.identificacion, " +
                        "       post.estado_postulacion " +
                        "FROM postulacion post " +
                        "JOIN postulante p ON post.id_postulante = p.id_postulante " +
                        "WHERE post.id_postulacion = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, idPostulacion);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    public Map<String, Object> obtenerResultadosPostulante(Long idUsuario) {
        return obtenerResultadosPostulanteConFiltro(idUsuario, null);
    }

    public Map<String, Object> obtenerResultadosPostulanteConFiltro(Long idUsuario, Long idPostulacion) {
        Map<String, Object> resultado = new HashMap<>();

        // ── 1. Proceso del postulante ─────────────────────────────────────────
        // Si se pasa idPostulacion se filtra por esa solicitud específica,
        // evitando traer el proceso de otra postulación del mismo usuario.
        final List<Map<String, Object>> procesos;
        if (idPostulacion != null) {
            String sqlProceso =
                    "SELECT pe.id_proceso, pe.estado_general, pe.decision, " +
                            "       pe.justificacion_decision, " +
                            "       pe.puntaje_matriz, pe.puntaje_entrevista, pe.puntaje_final, " +
                            "       p.nombres_postulante, p.apellidos_postulante, p.identificacion " +
                            "FROM proceso_evaluacion pe " +
                            "JOIN postulante p ON p.id_postulante = pe.id_postulante " +
                            "WHERE p.id_usuario = ? " +
                            "  AND pe.id_solicitud = (SELECT id_solicitud FROM postulacion WHERE id_postulacion = ?) " +
                            "ORDER BY pe.id_proceso DESC LIMIT 1";
            procesos = jdbcTemplate.queryForList(sqlProceso, idUsuario, idPostulacion);
        } else {
            // Sin filtro: toma el proceso activo más reciente (no completado ni cancelado)
            // para no traer un proceso de otra solicitud que esté más avanzado.
            String sqlProceso =
                    "SELECT pe.id_proceso, pe.estado_general, pe.decision, " +
                            "       pe.justificacion_decision, " +
                            "       pe.puntaje_matriz, pe.puntaje_entrevista, pe.puntaje_final, " +
                            "       p.nombres_postulante, p.apellidos_postulante, p.identificacion " +
                            "FROM proceso_evaluacion pe " +
                            "JOIN postulante p ON p.id_postulante = pe.id_postulante " +
                            "WHERE p.id_usuario = ? " +
                            "ORDER BY " +
                            "  CASE WHEN pe.estado_general NOT IN ('completado','cancelado') THEN 0 ELSE 1 END ASC, " +
                            "  pe.id_proceso DESC LIMIT 1";
            procesos = jdbcTemplate.queryForList(sqlProceso, idUsuario);
        }

        Map<String, Object> proceso = procesos.get(0);
        // Solo copiar campos no-puntaje directamente; los puntajes se recalculan abajo
        resultado.put("id_proceso",             proceso.get("id_proceso"));
        resultado.put("estado_general",         proceso.get("estado_general"));
        resultado.put("decision",               proceso.get("decision"));
        resultado.put("justificacion_decision", proceso.get("justificacion_decision"));
        resultado.put("nombres_postulante",     proceso.get("nombres_postulante"));
        resultado.put("apellidos_postulante",   proceso.get("apellidos_postulante"));
        resultado.put("identificacion",         proceso.get("identificacion"));

        Long idProceso = ((Number) proceso.get("id_proceso")).longValue();

        // ── 2. Puntajes por item de la matriz ─────────────────────────────────
        // Solo incluir puntajes si la matriz está realmente calificada:
        // se verifica comprobando si alguna fase está completada.
        String sqlPuntajes = "SELECT item_id, valor FROM matriz_meritos_puntaje WHERE id_proceso = ?";
        List<Map<String, Object>> puntajesRaw = jdbcTemplate.queryForList(sqlPuntajes, idProceso);

        Map<String, Double> puntajesMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> p : puntajesRaw) {
            String itemId   = (String) p.get("item_id");
            Object rawValor = p.get("valor");
            if (rawValor == null || itemId == null) continue;
            double valor;
            if (rawValor instanceof Number) {
                valor = ((Number) rawValor).doubleValue();
            } else {
                try { valor = Double.parseDouble(rawValor.toString().trim()); }
                catch (NumberFormatException e) { valor = 0.0; }
            }
            puntajesMap.put(itemId, valor);
        }
        resultado.put("puntajes", puntajesMap);

        // ── 3. Estructura de secciones e items de la matriz ───────────────────
        String sqlSecciones =
                "SELECT ms.id_seccion, ms.codigo AS seccion_codigo, ms.titulo AS seccion_titulo, " +
                        "       ms.puntaje_maximo AS seccion_maximo, ms.tipo AS seccion_tipo, ms.orden AS seccion_orden, " +
                        "       mi.id_item, mi.codigo AS item_codigo, mi.label AS item_label, " +
                        "       mi.puntaje_maximo AS item_maximo, mi.orden AS item_orden " +
                        "FROM matriz_seccion ms " +
                        "JOIN matriz_item mi ON mi.id_seccion = ms.id_seccion " +
                        "WHERE ms.activo = TRUE AND mi.activo = TRUE " +
                        "ORDER BY ms.orden ASC, mi.orden ASC";
        List<Map<String, Object>> seccionesRaw = jdbcTemplate.queryForList(sqlSecciones);

        java.util.Map<Long, Map<String, Object>> seccionesMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : seccionesRaw) {
            Long idSeccion = ((Number) row.get("id_seccion")).longValue();
            if (!seccionesMap.containsKey(idSeccion)) {
                Map<String, Object> sec = new java.util.LinkedHashMap<>();
                sec.put("codigo",   row.get("seccion_codigo"));
                sec.put("titulo",   row.get("seccion_titulo"));
                sec.put("maximo",   row.get("seccion_maximo"));
                sec.put("tipo",     row.get("seccion_tipo"));
                sec.put("orden",    row.get("seccion_orden"));
                sec.put("items",    new java.util.ArrayList<Map<String, Object>>());
                seccionesMap.put(idSeccion, sec);
            }
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("codigo", row.get("item_codigo"));
            item.put("label",  row.get("item_label"));
            item.put("maximo", row.get("item_maximo"));
            ((java.util.List<Map<String, Object>>) seccionesMap.get(idSeccion).get("items")).add(item);
        }
        resultado.put("secciones", new java.util.ArrayList<>(seccionesMap.values()));

        // ── 4. Fases del proceso — calificacion solo si la fase está completada ──
        // CRITICAL FIX: si la fase no está 'completada' u 'omitida', su calificacion
        // puede ser un residuo de un proceso anterior reiniciado. Se fuerza a NULL.
        String sqlFases =
                "SELECT fe.nombre AS fase_nombre, fe.tipo AS fase_tipo, " +
                        "       fe.peso AS fase_peso, fe.orden AS fase_orden, " +
                        "       fp.estado, " +
                        "       CASE WHEN fp.estado IN ('completada', 'omitida') " +
                        "            THEN fp.calificacion ELSE NULL END AS calificacion, " +
                        "       to_char(fp.fecha_completada, 'DD/MM/YYYY') AS fecha_completada " +
                        "FROM fase_proceso fp " +
                        "JOIN fase_evaluacion fe ON fe.id_fase = fp.id_fase " +
                        "WHERE fp.id_proceso = ? " +
                        "ORDER BY fe.orden ASC";
        List<Map<String, Object>> fases = jdbcTemplate.queryForList(sqlFases, idProceso);
        resultado.put("fasesDetalle", fases);

        // ── 5. Recalcular puntaje_final desde las fases completadas ──────────
        // No confiar en las columnas puntaje_matriz/entrevista/final de proceso_evaluacion
        // porque pueden tener datos residuales de un reinicio parcial.
        double sumFases = 0;
        for (Map<String, Object> f : fases) {
            Object cal = f.get("calificacion");
            if (cal != null) sumFases += ((Number) cal).doubleValue();
        }
        // puntaje_matriz desde columna solo si hay fases completadas
        // (así sabemos que el proceso está activo, no reiniciado con datos viejos)
        boolean hayFaseCompletada = fases.stream()
                .anyMatch(f -> "completada".equals(f.get("estado")));

        Object colMatriz = proceso.get("puntaje_matriz");
        double puntajeMatrizCol = colMatriz != null ? ((Number) colMatriz).doubleValue() : 0;

        // Si la suma de fases es 0 y la columna tiene valor, solo mostrar si hay fases completadas
        double puntajeFinalReal = hayFaseCompletada ? sumFases : 0;
        double puntajeMatrizReal = (hayFaseCompletada && puntajeMatrizCol > 0) ? puntajeMatrizCol : 0;
        if (puntajeMatrizReal > 0) puntajeFinalReal += puntajeMatrizReal;

        resultado.put("puntaje_final",      puntajeFinalReal > 0 ? puntajeFinalReal : null);
        resultado.put("puntaje_matriz",     puntajeMatrizReal > 0 ? puntajeMatrizReal : null);

        return resultado;
    }

}