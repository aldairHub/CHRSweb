package org.uteq.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.IntegridadResumenDTO;
import org.uteq.backend.service.IntegridadAuditoriaService;

@RestController
@RequestMapping("/api/admin/auditoria/integridad")
public class IntegridadAuditoriaController {

    private final IntegridadAuditoriaService service;

    public IntegridadAuditoriaController(IntegridadAuditoriaService service) {
        this.service = service;
    }

    /**
     * GET /api/admin/auditoria/integridad/verificar?limite=1000
     */
    @GetMapping("/verificar")
    public ResponseEntity<IntegridadResumenDTO> verificar(
            @RequestParam(defaultValue = "1000") int limite) {
        // Clamp entre 100 y 10_000
        limite = Math.max(100, Math.min(limite, 10_000));
        return ResponseEntity.ok(service.verificar(limite));
    }
}
