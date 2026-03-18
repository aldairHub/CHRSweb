package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluacion/evaluadores")
@CrossOrigin(origins = "*")
public class EvaluadoresController {

    private final JdbcTemplate jdbc;

    public EvaluadoresController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(
            @RequestParam(defaultValue = "EVALUADOR") String rol) {
        List<Map<String, Object>> evaluadores = jdbc.queryForList(
                "SELECT * FROM get_evaluadores_por_rol(?)", rol
        );
        return ResponseEntity.ok(evaluadores);
    }

    @GetMapping("/por-fase/{idFase}")
    public ResponseEntity<List<Map<String, Object>>> listarPorFase(
            @PathVariable Long idFase) {
        List<Map<String, Object>> evaluadores = jdbc.queryForList(
                "SELECT * FROM get_evaluadores_por_fase(?)", idFase
        );
        return ResponseEntity.ok(evaluadores);
    }

    @GetMapping("/por-solicitud")
    public ResponseEntity<List<Map<String, Object>>> listarPorSolicitud(
            @RequestParam Long idSolicitud) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT u.id_usuario, u.nombres, u.apellidos, u.correo, u.identificacion " +
                        "FROM usuario u " +
                        "INNER JOIN proceso_evaluacion pe ON pe.id_evaluador = u.id_usuario " +
                        "WHERE pe.id_solicitud = ? " +
                        "GROUP BY u.id_usuario, u.nombres, u.apellidos, u.correo, u.identificacion",
                idSolicitud
        );
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/asignar-materia")
    public ResponseEntity<Map<String, Object>> asignarPorMateria(
            @RequestBody Map<String, Object> body) {
        Long idEvaluador = toLong(body.get("idEvaluador"));
        Long idSolicitud = toLong(body.get("idSolicitud"));

        if (idEvaluador == null || idSolicitud == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("exito", false, "mensaje", "idEvaluador e idSolicitud son requeridos"));
        }

        // Verificar si ya está asignado
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM proceso_evaluacion WHERE id_evaluador = ? AND id_solicitud = ?",
                Integer.class, idEvaluador, idSolicitud
        );

        if (existe != null && existe > 0) {
            return ResponseEntity.ok(
                    Map.of("exito", false, "mensaje", "El evaluador ya está asignado a esta materia")
            );
        }

        List<Map<String, Object>> postulaciones = jdbc.queryForList(
                "SELECT id_postulacion FROM postulacion WHERE id_solicitud = ?", idSolicitud
        );

        if (postulaciones.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of("exito", false, "mensaje", "No hay postulaciones activas para esta materia")
            );
        }

        int asignados = 0;
        for (Map<String, Object> post : postulaciones) {
            Long idPostulacion = toLong(post.get("id_postulacion"));
            try {
                jdbc.update(
                        "INSERT INTO proceso_evaluacion (id_postulacion, id_solicitud, id_evaluador, estado) " +
                                "VALUES (?, ?, ?, 'pendiente') " +
                                "ON CONFLICT (id_postulacion, id_evaluador) DO NOTHING",
                        idPostulacion, idSolicitud, idEvaluador
                );
                asignados++;
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", "Evaluador asignado correctamente a " + asignados + " postulación(es)"
        ));
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Number)  return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}