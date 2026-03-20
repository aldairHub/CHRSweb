package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.ComiteFinalService;

import java.util.Map;

@RestController
@RequestMapping("/api/comite-final")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ComiteFinalController {

    private final ComiteFinalService service;

    // GET /api/comite-final/solicitud/{idSolicitud}/candidatos
    @GetMapping("/solicitud/{idSolicitud}/candidatos")
    public ResponseEntity<?> candidatos(@PathVariable Long idSolicitud) {
        return ResponseEntity.ok(service.obtenerCandidatosComite(idSolicitud));
    }

    // GET /api/comite-final/solicitud/{idSolicitud}/confirmado
    @GetMapping("/solicitud/{idSolicitud}/confirmado")
    public ResponseEntity<?> estaConfirmado(@PathVariable Long idSolicitud) {
        return ResponseEntity.ok(Map.of("confirmado", service.estaConfirmado(idSolicitud)));
    }

    // POST /api/comite-final/solicitud/{idSolicitud}/confirmar
    @PostMapping("/solicitud/{idSolicitud}/confirmar")
    public ResponseEntity<?> confirmar(
            @PathVariable Long idSolicitud,
            @RequestBody Map<String, Object> body) {
        Long idProcesoGanador = ((Number) body.get("idProcesoGanador")).longValue();
        String actaComite     = (String) body.get("actaComite");

        if (actaComite == null || actaComite.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "El acta del comité es obligatoria."));
        }

        service.confirmarDecision(idSolicitud, idProcesoGanador, actaComite);
        return ResponseEntity.ok(Map.of("mensaje", "Decisión final confirmada y enviada al Vicerrectorado."));
    }

    // GET /api/comite-final/revisor/decisiones?estado=pendiente
    @GetMapping("/revisor/decisiones")
    public ResponseEntity<?> decisionesRevisor(
            @RequestParam(defaultValue = "pendiente") String estado) {
        return ResponseEntity.ok(service.listarDecisionesRevisor(estado));
    }

    // PATCH /api/comite-final/revisor/decisiones/{idDecision}/revisar
    @PatchMapping("/revisor/decisiones/{idDecision}/revisar")
    public ResponseEntity<?> marcarRevisada(@PathVariable Long idDecision) {
        service.marcarRevisada(idDecision);
        return ResponseEntity.ok(Map.of("mensaje", "Decisión marcada como revisada."));
    }
}
