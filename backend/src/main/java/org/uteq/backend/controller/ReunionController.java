package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ReunionRequestDTO;
import org.uteq.backend.dto.ReunionResponseDTO;
import org.uteq.backend.service.ReunionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluacion/reuniones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReunionController {

    private final ReunionService service;

    /**
     * GET /api/evaluacion/reuniones?estado=programada
     * AGREGADO: el frontend llama con ?estado= como param opcional.
     * Delega a listarProgramadas() si estado=programada, o devuelve todas.
     */
    @GetMapping
    public ResponseEntity<List<ReunionResponseDTO>> listar(
            @RequestParam(required = false) String estado) {
        if ("programada".equals(estado)) {
            return ResponseEntity.ok(service.listarProgramadas());
        }
        return ResponseEntity.ok(service.listarProgramadas()); // por ahora devuelve todas las programadas
    }

    /** GET /api/evaluacion/reuniones/programadas (se mantiene) */
    @GetMapping("/programadas")
    public ResponseEntity<List<ReunionResponseDTO>> listarProgramadas() {
        return ResponseEntity.ok(service.listarProgramadas());
    }

    @GetMapping("/mi-entrevista")
    public ResponseEntity<ReunionResponseDTO> miEntrevista(@RequestParam Long idUsuario) {
        ReunionResponseDTO dto = service.obtenerMiEntrevista(idUsuario);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    /** GET /api/evaluacion/reuniones/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ReunionResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    /** POST /api/evaluacion/reuniones */
    @PostMapping
    public ResponseEntity<ReunionResponseDTO> programar(@RequestBody ReunionRequestDTO dto) {
        return ResponseEntity.status(201).body(service.programar(dto));
    }

    /**
     * PATCH /api/evaluacion/reuniones/{id}/estado?estado=completada
     * AGREGADO: el frontend llama a este endpoint para cambiar estado.
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReunionResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    /** PATCH /api/evaluacion/reuniones/{id}/cancelar (se mantiene) */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReunionResponseDTO> cancelar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(service.cancelar(id, motivo));
    }
}
