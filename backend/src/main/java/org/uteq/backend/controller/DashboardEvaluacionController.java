package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.DashboardEvaluacionDTO;
import org.uteq.backend.service.DashboardEvaluacionService;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardEvaluacionController {

    private final DashboardEvaluacionService service;

    /**
     * GET /api/evaluacion/procesos/dashboard
     * El frontend llama a esta URL exacta desde dashboard.service.ts
     */
    @GetMapping("/api/evaluacion/procesos/dashboard")
    public ResponseEntity<DashboardEvaluacionDTO> stats() {
        return ResponseEntity.ok(service.obtenerStats());
    }
}
