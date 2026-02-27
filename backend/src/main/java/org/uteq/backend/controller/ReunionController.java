package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ReunionRequestDTO;
import org.uteq.backend.dto.ReunionResponseDTO;
import org.uteq.backend.service.ReunionService;

import java.util.List;
import java.util.Map;

/**
 * Gestión de Reuniones de Evaluación.
 * Endpoints consumidos por el componente programar-reunion del frontend.
 */
@RestController
@RequestMapping("/api/evaluacion/reuniones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReunionController {

    private final ReunionService service;

    /** Lista las reuniones programadas (próximas) */
    @GetMapping("/programadas")
    public ResponseEntity<List<ReunionResponseDTO>> listarProgramadas() {
        return ResponseEntity.ok(service.listarProgramadas());
    }

    /** Obtener reunión por ID */
    @GetMapping("/{id}")
    public ResponseEntity<ReunionResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    /**
     * Programar (crear o actualizar) una reunión.
     * Body: { idPostulante, idFase, fecha, hora, duracion, modalidad, enlace, evaluadoresIds, observaciones }
     */
    @PostMapping
    public ResponseEntity<ReunionResponseDTO> programar(@RequestBody ReunionRequestDTO dto) {
        return ResponseEntity.status(201).body(service.programar(dto));
    }

    /**
     * Cancelar una reunión.
     * Body: { "motivo": "..." }  (opcional)
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReunionResponseDTO> cancelar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(service.cancelar(id, motivo));
    }
}
