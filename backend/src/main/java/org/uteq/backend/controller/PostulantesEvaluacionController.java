package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.DecisionRequestDTO;
import org.uteq.backend.dto.PostulanteDetalleDTO;
import org.uteq.backend.dto.PostulanteEvaluacionDTO;
import org.uteq.backend.dto.ResultadoPostulanteDTO;
import org.uteq.backend.service.EvaluacionService;
import org.uteq.backend.service.ProcesoEvaluacionService;

import java.util.List;
import java.util.Map;

/**
 * CAMBIO: @RequestMapping era "/api/evaluacion/postulantes"
 *         ahora es "/api/evaluacion/procesos" para coincidir con el frontend.
 */
@RestController
@RequestMapping("/api/evaluacion/procesos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PostulantesEvaluacionController {

    private final ProcesoEvaluacionService procesoService;
    private final EvaluacionService evaluacionService;

    /**
     * GET /api/evaluacion/procesos?estado=en_proceso&query=...
     * El frontend puede pasar estado y query como filtros opcionales.
     * El service actual no filtra, pero los recibe sin error.
     */
    @GetMapping
    public ResponseEntity<List<PostulanteEvaluacionDTO>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(procesoService.listarPostulantes());
    }

    /** GET /api/evaluacion/procesos/{idProceso} */
    @GetMapping("/{idProceso}")
    public ResponseEntity<PostulanteDetalleDTO> detalle(@PathVariable Long idProceso) {
        return ResponseEntity.ok(procesoService.obtenerDetalle(idProceso));
    }

    /** GET /api/evaluacion/procesos/{idProceso}/resultados */
    @GetMapping("/{idProceso}/resultados")
    public ResponseEntity<ResultadoPostulanteDTO> resultados(@PathVariable Long idProceso) {
        return ResponseEntity.ok(evaluacionService.obtenerResultados(idProceso));
    }

    /** POST /api/evaluacion/procesos/{idProceso}/decision */
    @PostMapping("/{idProceso}/decision")
    public ResponseEntity<Map<String, String>> registrarDecision(
            @PathVariable Long idProceso,
            @RequestBody DecisionRequestDTO dto) {
        procesoService.registrarDecision(idProceso, dto);
        return ResponseEntity.ok(Map.of(
                "mensaje",  "Decisión registrada correctamente",
                "decision", dto.getDecision()
        ));
    }
}
