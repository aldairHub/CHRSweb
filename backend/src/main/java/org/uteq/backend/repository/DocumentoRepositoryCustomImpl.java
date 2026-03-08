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
                    result.put("exitoso", rs.getBoolean("v_exitoso"));
                    result.put("mensaje", rs.getString("v_mensaje"));
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

}
