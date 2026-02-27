package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.FaseEvaluacionRequestDTO;
import org.uteq.backend.dto.FaseEvaluacionResponseDTO;
import org.uteq.backend.service.FaseEvaluacionService;

import java.util.List;

/**
 * Gestión de las Fases de Evaluación (configuración del proceso).
 * Endpoints consumidos por el componente config-fases del frontend.
 */
@RestController
@RequestMapping("/api/evaluacion/fases")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FaseEvaluacionController {

    private final FaseEvaluacionService service;

    /** Listar todas las fases ordenadas por orden */
    @GetMapping
    public ResponseEntity<List<FaseEvaluacionResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /** Obtener una fase por ID */
    @GetMapping("/{id}")
    public ResponseEntity<FaseEvaluacionResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    /** Crear fase */
    @PostMapping
    public ResponseEntity<FaseEvaluacionResponseDTO> crear(@RequestBody FaseEvaluacionRequestDTO dto) {
        return ResponseEntity.status(201).body(service.crear(dto));
    }

    /** Actualizar fase */
    @PutMapping("/{id}")
    public ResponseEntity<FaseEvaluacionResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody FaseEvaluacionRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    /** Activar / desactivar fase */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean estado) {
        FaseEvaluacionRequestDTO dto = new FaseEvaluacionRequestDTO();
        dto.setEstado(estado);
        service.actualizar(id, dto);
        return ResponseEntity.ok().build();
    }

    /** Eliminar fase */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
