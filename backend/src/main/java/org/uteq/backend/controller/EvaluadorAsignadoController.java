package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.EvaluadorAsignadoService;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/evaluadores-asignados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EvaluadorAsignadoController {

    private final EvaluadorAsignadoService service;

    @GetMapping("/solicitud/{idSolicitud}/procesos")
    public ResponseEntity<?> procesosDeSolicitud(@PathVariable Long idSolicitud) {
        List<Map<String, Object>> procesos = service.obtenerProcesosPorSolicitud(idSolicitud);
        return ResponseEntity.ok(procesos);
    }


    // GET /api/evaluadores-asignados/{idProceso}?tipo=matriz
    @GetMapping("/{idProceso}")
    public ResponseEntity<?> listarAsignados(
            @PathVariable Long idProceso,
            @RequestParam(defaultValue = "matriz") String tipo) {
        return ResponseEntity.ok(service.listarEvaluadoresProceso(idProceso, tipo));
    }

    // GET /api/evaluadores-asignados/{idProceso}/disponibles?tipo=matriz[&idSolicitud=X]
    @GetMapping("/{idProceso}/disponibles")
    public ResponseEntity<?> listarDisponibles(
            @PathVariable Long idProceso,
            @RequestParam(defaultValue = "matriz") String tipo,
            @RequestParam(required = false) Long idSolicitud) {
        if (idSolicitud != null) {
            return ResponseEntity.ok(service.listarDisponiblesPorFacultad(idProceso, tipo, idSolicitud));
        }
        return ResponseEntity.ok(service.listarDisponibles(idProceso, tipo));
    }

    // POST /api/evaluadores-asignados/{idProceso}
    @PostMapping("/{idProceso}")
    public ResponseEntity<?> asignar(
            @PathVariable Long idProceso,
            @RequestBody Map<String, Object> body) {
        Long idUsuario = ((Number) body.get("idUsuario")).longValue();
        String tipo    = (String) body.getOrDefault("tipo", "matriz");
        service.asignar(idProceso, idUsuario, tipo);
        return ResponseEntity.ok(Map.of("mensaje", "Evaluador asignado correctamente"));
    }

    // DELETE /api/evaluadores-asignados/{idProceso}/{idUsuario}?tipo=matriz
    @DeleteMapping("/{idProceso}/{idUsuario}")
    public ResponseEntity<?> quitar(
            @PathVariable Long idProceso,
            @PathVariable Long idUsuario,
            @RequestParam(defaultValue = "matriz") String tipo) {
        service.quitar(idProceso, idUsuario, tipo);
        return ResponseEntity.ok(Map.of("mensaje", "Evaluador removido correctamente"));
    }

    // GET /api/evaluadores-asignados/{idProceso}/puede-evaluar?tipo=matriz
    // Verifica si el usuario autenticado puede evaluar este proceso
    @GetMapping("/{idProceso}/puede-evaluar")
    public ResponseEntity<?> puedeEvaluar(
            @PathVariable Long idProceso,
            @RequestParam(defaultValue = "matriz") String tipo,
            Authentication auth) {
        String usuarioApp = auth.getName();
        Long idUsuario = service.obtenerIdUsuario(usuarioApp);
        if (idUsuario == null) {
            return ResponseEntity.ok(Map.of("puedeEvaluar", false));
        }
        boolean puede = service.puedeEvaluar(idProceso, idUsuario, tipo);
        return ResponseEntity.ok(Map.of("puedeEvaluar", puede, "idUsuario", idUsuario));
    }
}
