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
 * Gestión de Postulantes dentro del proceso de evaluación.
 * Endpoints consumidos por los componentes postulantes, resultados y dashboard.
 */
@RestController
@RequestMapping("/api/evaluacion/postulantes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PostulantesEvaluacionController {

    private final ProcesoEvaluacionService procesoService;
    private final EvaluacionService evaluacionService;

    /** Lista todos los postulantes activos en proceso de evaluación */
    @GetMapping
    public ResponseEntity<List<PostulanteEvaluacionDTO>> listar() {
        return ResponseEntity.ok(procesoService.listarPostulantes());
    }

    /** Obtiene el detalle de un postulante (fases + historial) */
    @GetMapping("/{idProceso}")
    public ResponseEntity<PostulanteDetalleDTO> detalle(@PathVariable Long idProceso) {
        return ResponseEntity.ok(procesoService.obtenerDetalle(idProceso));
    }

    /** Obtiene los resultados y calificaciones de un postulante */
    @GetMapping("/{idProceso}/resultados")
    public ResponseEntity<ResultadoPostulanteDTO> resultados(@PathVariable Long idProceso) {
        return ResponseEntity.ok(evaluacionService.obtenerResultados(idProceso));
    }

    /**
     * Registra la decisión final del comité sobre un postulante.
     * Body: { "decision": "aprobado_contratar", "justificacion": "..." }
     */
    @PostMapping("/{idProceso}/decision")
    public ResponseEntity<Map<String, String>> registrarDecision(
            @PathVariable Long idProceso,
            @RequestBody DecisionRequestDTO dto) {
        procesoService.registrarDecision(idProceso, dto);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Decisión registrada correctamente",
                "decision", dto.getDecision()
        ));
    }
}
