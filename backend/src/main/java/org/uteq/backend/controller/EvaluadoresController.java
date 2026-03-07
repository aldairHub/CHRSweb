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

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(
            @RequestParam(defaultValue = "EVALUADOR") String rol) {

        List<Map<String, Object>> evaluadores = jdbc.queryForList(
                "select * from get_evaluadores_por_rol(?)", rol
        );

        return ResponseEntity.ok(evaluadores);
    }
}