package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.ComiteFinalService;
import org.uteq.backend.service.ReporteMatrizService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;

@RestController
@RequestMapping("/api/comite-final")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ComiteFinalController {

    private final ComiteFinalService service;
    private final ReporteMatrizService reporteService;

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

    // GET /api/comite-final/solicitud/{idSolicitud}/es-solicitante
    // Verifica si el usuario autenticado es quien realizó la solicitud
    @GetMapping("/solicitud/{idSolicitud}/es-solicitante")
    public ResponseEntity<?> esSolicitante(
            @PathVariable Long idSolicitud,
            Authentication auth) {
        if (auth == null) return ResponseEntity.ok(Map.of("esSolicitante", false));
        boolean es = service.esSolicitante(idSolicitud, auth.getName());
        return ResponseEntity.ok(Map.of("esSolicitante", es));
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

// ════════════════════════════════════════════════════════════════════════
// ENDPOINTS NUEVOS — agregar a ComiteFinalController.java
// ════════════════════════════════════════════════════════════════════════

    // POST /api/comite-final/solicitud/{idSolicitud}/notificar
    @PostMapping("/solicitud/{idSolicitud}/notificar")
    public ResponseEntity<?> notificar(@PathVariable Long idSolicitud) {
        try {
            service.notificarDecision(idSolicitud);
            service.enviarCorreosDecision(idSolicitud);  // asíncrono
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Notificaciones enviadas. Los correos se procesarán en segundo plano."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("mensaje", "Error al notificar: " + e.getMessage()));
        }
    }

    // GET /api/comite-final/solicitud/{idSolicitud}/acta-pdf
    @GetMapping("/solicitud/{idSolicitud}/acta-pdf")
    public ResponseEntity<byte[]> generarActa(@PathVariable Long idSolicitud) {
        byte[] pdf = reporteService.generarActa(idSolicitud);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename=\"acta-meritos-" + idSolicitud + ".pdf\"")
                .body(pdf);
    }

    // GET /api/comite-final/solicitud/{idSolicitud}/informe-pdf
    @GetMapping("/solicitud/{idSolicitud}/informe-pdf")
    public ResponseEntity<byte[]> generarInforme(@PathVariable Long idSolicitud) {
        byte[] pdf = reporteService.generarInformeFinal(idSolicitud);
        service.enviarInformeAAutoridad(idSolicitud, pdf);  // asíncrono
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename=\"informe-seleccion-" + idSolicitud + ".pdf\"")
                .body(pdf);
    }

    // GET /api/comite-final/revisor/decisiones/{idSolicitud}/detalle
    @GetMapping("/revisor/decisiones/{idSolicitud}/detalle")
    public ResponseEntity<?> detalleDecision(@PathVariable Long idSolicitud) {
        return ResponseEntity.ok(service.obtenerDetalleDecision(idSolicitud));
    }
}
