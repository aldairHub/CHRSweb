package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.PostulanteListaDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/postulaciones/evaluador")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PostulantesEvaluadorController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * GET /api/postulaciones/evaluador/lista
     * Lista todas las postulaciones activas para el evaluador
     */
    @GetMapping("/lista")
    public ResponseEntity<List<PostulanteListaDTO>> listar() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM public.sp_listar_postulantes_evaluador()"
        );

        List<PostulanteListaDTO> lista = rows.stream().map(row -> {
            PostulanteListaDTO dto = new PostulanteListaDTO();
            dto.setIdPostulacion(toLong(row.get("id_postulacion")));
            dto.setIdPostulante(toLong(row.get("id_postulante")));
            dto.setIdentificacion((String) row.get("identificacion"));
            dto.setNombresPostulante((String) row.get("nombres_postulante"));
            dto.setApellidosPostulante((String) row.get("apellidos_postulante"));
            dto.setCorreoPostulante((String) row.get("correo_postulante"));
            dto.setEstadoPostulacion((String) row.get("estado_postulacion"));
            dto.setNombreMateria((String) row.get("nombre_materia"));
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return Long.parseLong(val.toString());
    }
}
