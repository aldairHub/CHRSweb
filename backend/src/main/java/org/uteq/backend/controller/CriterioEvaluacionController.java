package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.CriterioEvaluacionRequestDTO;
import org.uteq.backend.dto.CriterioEvaluacionResponseDTO;
import org.uteq.backend.service.CriterioEvaluacionService;

import java.util.List;

/**
 * Gestión de Criterios de Evaluación por Plantilla.
 * Endpoints consumidos por el componente config-criterios del frontend.
 * La ruta incluye el idPlantilla para que el frontend pueda navegar directamente:
 * /api/evaluacion/plantillas/{idPlantilla}/criterios
 */
@RestController
@RequestMapping("/api/evaluacion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CriterioEvaluacionController {

    private final CriterioEvaluacionService service;

    /** Listar criterios de una plantilla */
    @GetMapping("/plantillas/{idPlantilla}/criterios")
    public ResponseEntity<List<CriterioEvaluacionResponseDTO>> listar(
            @PathVariable Long idPlantilla) {
        return ResponseEntity.ok(service.listarPorPlantilla(idPlantilla));
    }

    /** Obtener un criterio por ID */
    @GetMapping("/criterios/{id}")
    public ResponseEntity<CriterioEvaluacionResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    /** Crear criterio (el idPlantilla viene en el body) */
    @PostMapping("/criterios")
    public ResponseEntity<CriterioEvaluacionResponseDTO> crear(
            @RequestBody CriterioEvaluacionRequestDTO dto) {
        return ResponseEntity.status(201).body(service.crear(dto));
    }

    /** Actualizar criterio */
    @PutMapping("/criterios/{id}")
    public ResponseEntity<CriterioEvaluacionResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody CriterioEvaluacionRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    /** Eliminar criterio */
    @DeleteMapping("/criterios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
