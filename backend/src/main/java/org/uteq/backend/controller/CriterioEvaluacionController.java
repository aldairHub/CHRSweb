package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.CriterioEvaluacionRequestDTO;
import org.uteq.backend.dto.CriterioEvaluacionResponseDTO;
import org.uteq.backend.entity.FaseEvaluacion;
import org.uteq.backend.entity.PlantillaEvaluacion;
import org.uteq.backend.repository.FaseEvaluacionRepository;
import org.uteq.backend.service.CriterioEvaluacionService;

import java.util.List;

@RestController
@RequestMapping("/api/evaluacion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CriterioEvaluacionController {

    private final CriterioEvaluacionService service;
    private final FaseEvaluacionRepository faseRepository;

    /** GET /api/evaluacion/plantillas/{idPlantilla}/criterios */
    @GetMapping("/plantillas/{idPlantilla}/criterios")
    public ResponseEntity<List<CriterioEvaluacionResponseDTO>> listar(
            @PathVariable Long idPlantilla) {
        return ResponseEntity.ok(service.listarPorPlantilla(idPlantilla));
    }

    /** GET /api/evaluacion/criterios/por-fase/{idFase}
     *  Busca la plantilla asociada a la fase y devuelve sus criterios */
    @GetMapping("/criterios/por-fase/{idFase}")
    public ResponseEntity<List<CriterioEvaluacionResponseDTO>> listarPorFase(
            @PathVariable Long idFase) {

        FaseEvaluacion fase = faseRepository.findById(idFase)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada: " + idFase));

        PlantillaEvaluacion plantilla = fase.getPlantilla();
        if (plantilla == null) {
            return ResponseEntity.ok(List.of()); // fase sin plantilla → lista vacía
        }

        return ResponseEntity.ok(service.listarPorPlantilla(plantilla.getIdPlantilla()));
    }

    /** GET /api/evaluacion/criterios/{id} */
    @GetMapping("/criterios/{id}")
    public ResponseEntity<CriterioEvaluacionResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    /** POST /api/evaluacion/criterios */
    @PostMapping("/criterios")
    public ResponseEntity<CriterioEvaluacionResponseDTO> crear(
            @RequestBody CriterioEvaluacionRequestDTO dto) {
        return ResponseEntity.status(201).body(service.crear(dto));
    }

    /** PUT /api/evaluacion/criterios/{id} */
    @PutMapping("/criterios/{id}")
    public ResponseEntity<CriterioEvaluacionResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody CriterioEvaluacionRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    /** DELETE /api/evaluacion/criterios/{id} */
    @DeleteMapping("/criterios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}