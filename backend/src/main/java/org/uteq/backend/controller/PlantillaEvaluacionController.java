package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.PlantillaEvaluacionRequestDTO;
import org.uteq.backend.dto.PlantillaEvaluacionResponseDTO;
import org.uteq.backend.service.PlantillaEvaluacionService;

import java.util.List;

/**
 * Gestión de Plantillas de Evaluación.
 * Endpoints consumidos por el componente config-plantillas del frontend.
 */
@RestController
@RequestMapping("/api/evaluacion/plantillas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PlantillaEvaluacionController {

    private final PlantillaEvaluacionService service;

    @GetMapping
    public ResponseEntity<List<PlantillaEvaluacionResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaEvaluacionResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PlantillaEvaluacionResponseDTO> crear(
            @RequestBody PlantillaEvaluacionRequestDTO dto) {
        return ResponseEntity.status(201).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaEvaluacionResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody PlantillaEvaluacionRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
