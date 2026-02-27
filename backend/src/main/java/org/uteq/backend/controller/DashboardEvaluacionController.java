package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.DashboardEvaluacionDTO;
import org.uteq.backend.service.DashboardEvaluacionService;

/**
 * Dashboard de estadísticas del módulo de evaluación.
 * Endpoint consumido por el componente dashboard del frontend.
 */
@RestController
@RequestMapping("/api/evaluacion/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardEvaluacionController {

    private final DashboardEvaluacionService service;

    @GetMapping("/stats")
    public ResponseEntity<DashboardEvaluacionDTO> stats() {
        return ResponseEntity.ok(service.obtenerStats());
    }
}
