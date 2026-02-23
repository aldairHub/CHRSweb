package org.uteq.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.uteq.backend.dto.TipoDocumentoDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TipoDocumentoRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    public TipoDocumentoRepositoryCustom(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // SP 1: Listar
    public List<TipoDocumentoDTO> listar() {
        return jdbcTemplate.query(
                "SELECT * FROM sp_listar_tipos_documento()",
                (rs, row) -> {
                    TipoDocumentoDTO dto = new TipoDocumentoDTO();
                    dto.setIdTipoDocumento(rs.getLong("id_tipo_documento"));
                    dto.setNombre(rs.getString("nombre"));
                    dto.setDescripcion(rs.getString("descripcion"));
                    dto.setObligatorio(rs.getBoolean("obligatorio"));
                    dto.setActivo(rs.getBoolean("activo"));
                    return dto;
                }
        );
    }

    // SP 2: Crear
    public Map<String, Object> crear(String nombre, String descripcion, Boolean obligatorio) {
        Map<String, Object> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT v_id, v_mensaje FROM sp_crear_tipo_documento(?, ?, ?)",
                new Object[]{nombre, descripcion, obligatorio},
                rs -> {
                    result.put("id", rs.getLong("v_id"));
                    result.put("mensaje", rs.getString("v_mensaje"));
                }
        );
        return result;
    }

    // SP 3: Editar
    public Map<String, Object> editar(Long id, String nombre, String descripcion, Boolean obligatorio) {
        Map<String, Object> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT v_exitoso, v_mensaje FROM sp_editar_tipo_documento(?, ?, ?, ?)",
                new Object[]{id, nombre, descripcion, obligatorio},
                rs -> {
                    result.put("exitoso", rs.getBoolean("v_exitoso"));
                    result.put("mensaje", rs.getString("v_mensaje"));
                }
        );
        return result;
    }

    // SP 4: Toggle activo
    public Map<String, Object> toggle(Long id) {
        Map<String, Object> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT v_exitoso, v_activo, v_mensaje FROM sp_toggle_tipo_documento(?)",
                new Object[]{id},
                rs -> {
                    result.put("exitoso", rs.getBoolean("v_exitoso"));
                    result.put("activo", rs.getBoolean("v_activo"));
                    result.put("mensaje", rs.getString("v_mensaje"));
                }
        );
        return result;
    }
}
