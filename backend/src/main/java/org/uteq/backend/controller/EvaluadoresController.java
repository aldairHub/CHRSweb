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
@RequiredArgsConstructor
public class EvaluadoresController {

    private final JdbcTemplate jdbc;

    /** GET /api/evaluacion/evaluadores?rol=EVALUADOR */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(
            @RequestParam(defaultValue = "EVALUADOR") String rol) {

        List<Map<String, Object>> evaluadores = jdbc.queryForList(
                "SELECT * FROM get_evaluadores_por_rol(?)", rol
        );
        return ResponseEntity.ok(evaluadores);
    }

    /** GET /api/evaluacion/evaluadores/por-fase/{idFase} */
    @GetMapping("/por-fase/{idFase}")
    public ResponseEntity<List<Map<String, Object>>> listarPorFase(
            @PathVariable Long idFase) {

        List<Map<String, Object>> evaluadores = jdbc.queryForList(
                "SELECT * FROM get_evaluadores_por_fase(?)", idFase
        );
        return ResponseEntity.ok(evaluadores);
    }
}