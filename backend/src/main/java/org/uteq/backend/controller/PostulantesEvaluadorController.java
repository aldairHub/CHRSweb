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

    @GetMapping("/convocatorias")
    public ResponseEntity<List<Map<String, Object>>> listarConvocatorias() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DISTINCT c.id_convocatoria, c.titulo, c.estado_convocatoria, " +
                        "c.fecha_inicio, c.fecha_fin " +
                        "FROM convocatoria c " +
                        "INNER JOIN convocatoria_solicitud cs ON cs.id_convocatoria = c.id_convocatoria " +
                        "INNER JOIN solicitud_docente sd ON sd.id_solicitud = cs.id_solicitud " +
                        "INNER JOIN postulacion p ON p.id_solicitud = sd.id_solicitud " +
                        "ORDER BY c.fecha_inicio DESC"
        );
        return ResponseEntity.ok(rows);
    }

    /**
     * GET /api/postulaciones/evaluador/solicitudes?idConvocatoria={id}
     * Lista las solicitudes (materias) de una convocatoria que tienen postulantes
     */
    @GetMapping("/solicitudes")
    public ResponseEntity<List<Map<String, Object>>> listarSolicitudes(
            @RequestParam Long idConvocatoria) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT sd.id_solicitud, m.nombre_materia, car.nombre_carrera, " +
                        "f.nombre_facultad, sd.cantidad_docentes, sd.nivel_academico, " +
                        "COUNT(p.id_postulacion) AS total_postulantes " +
                        "FROM solicitud_docente sd " +
                        "INNER JOIN convocatoria_solicitud cs ON cs.id_solicitud = sd.id_solicitud " +
                        "INNER JOIN materia m ON m.id_materia = sd.id_materia " +
                        "INNER JOIN carrera car ON car.id_carrera = sd.id_carrera " +
                        "INNER JOIN facultad f ON f.id_facultad = car.id_facultad " +
                        "LEFT JOIN postulacion p ON p.id_solicitud = sd.id_solicitud " +
                        "WHERE cs.id_convocatoria = ? " +
                        "GROUP BY sd.id_solicitud, m.nombre_materia, car.nombre_carrera, " +
                        "f.nombre_facultad, sd.cantidad_docentes, sd.nivel_academico " +
                        "ORDER BY m.nombre_materia",
                idConvocatoria
        );
        return ResponseEntity.ok(rows);
    }

    /**
     * GET /api/postulaciones/evaluador/lista
     * Lista postulaciones, opcionalmente filtradas por solicitud
     */
    @GetMapping("/lista")
    public ResponseEntity<List<PostulanteListaDTO>> listar(
            @RequestParam(required = false) Long idSolicitud) {
        List<Map<String, Object>> rows;
        if (idSolicitud != null) {
            rows = jdbcTemplate.queryForList(
                    "SELECT p.id_postulacion, pos.id_postulante, " +
                            "pos.identificacion, pos.nombres_postulante, " +
                            "pos.apellidos_postulante, pos.correo_postulante, " +
                            "p.estado_postulacion, m.nombre_materia " +
                            "FROM postulacion p " +
                            "INNER JOIN postulante pos ON pos.id_postulante = p.id_postulante " +
                            "INNER JOIN solicitud_docente sd ON sd.id_solicitud = p.id_solicitud " +
                            "INNER JOIN materia m ON m.id_materia = sd.id_materia " +
                            "WHERE p.id_solicitud = ? " +
                            "ORDER BY pos.apellidos_postulante",
                    idSolicitud
            );
        } else {
            rows = jdbcTemplate.queryForList(
                    "SELECT * FROM public.sp_listar_postulantes_evaluador()"
            );
        }

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