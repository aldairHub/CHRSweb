package org.uteq.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.MonitorResumenDTO;
import org.uteq.backend.service.MonitorQueriesService;

@RestController
@RequestMapping("/api/admin/auditoria/monitor")
public class MonitorQueriesController {

    private final MonitorQueriesService service;

    public MonitorQueriesController(MonitorQueriesService service) {
        this.service = service;
    }

    /**
     * GET /api/admin/auditoria/monitor/queries?orden=tiempo_promedio&limite=20
     * orden: tiempo_promedio | tiempo_total | llamadas
     */
    @GetMapping("/queries")
    public ResponseEntity<MonitorResumenDTO> queries(
            @RequestParam(defaultValue = "tiempo_promedio") String orden,
            @RequestParam(defaultValue = "20") int limite) {
        limite = Math.max(5, Math.min(limite, 100));
        return ResponseEntity.ok(service.obtener(orden, limite));
    }

    /**
     * POST /api/admin/auditoria/monitor/resetear
     */
    @PostMapping("/resetear")
    public ResponseEntity<Void> resetear() {
        service.resetear();
        return ResponseEntity.noContent().build();
    }
}
