package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.RankingPrepostulacionesRequestDTO;
import org.uteq.backend.dto.RankingResultadoDTO;
import org.uteq.backend.service.MistralRankingService;

import java.util.List;
import java.util.Map;

/**
 * Endpoint para el análisis IA de pre-postulantes usando Mistral AI.
 *
 * POST /api/admin/prepostulaciones/ia/ranking
 *
 * Body:
 * {
 *   "idSolicitud": 5,
 *   "analizarDocumentos": true,
 *   "analizarNivelAcademico": true
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/prepostulaciones/ia")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RankingIaController {

    private final MistralRankingService mistralRankingService;

    @PostMapping("/ranking")
    public ResponseEntity<?> generarRanking(
            @RequestBody RankingPrepostulacionesRequestDTO request) {

        log.info("[IA-RANKING] Solicitud recibida para idSolicitud={}, docs={}, nivel={}",
                request.getIdSolicitud(),
                request.isAnalizarDocumentos(),
                request.isAnalizarNivelAcademico());

        // Validaciones básicas
        if (request.getIdSolicitud() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El campo idSolicitud es obligatorio."));
        }

        if (!request.isAnalizarDocumentos() && !request.isAnalizarNivelAcademico()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debes seleccionar al menos un criterio de análisis."));
        }

        try {
            List<RankingResultadoDTO> ranking = mistralRankingService.generarRanking(request);

            if (ranking.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "ranking",        List.of(),
                        "mensaje",        "No se encontraron pre-postulantes para esta solicitud.",
                        "resumenGeneral", ""
                ));
            }

            // El resumenGeneral viene en todos los items (mismo valor), lo extraemos una sola vez
            String resumenGeneral = ranking.get(0).getResumenGeneral();

            // Limpiamos resumenGeneral de los items individuales para no duplicar en el frontend
            ranking.forEach(r -> r.setResumenGeneral(null));

            log.info("[IA-RANKING] Ranking generado exitosamente: {} candidatos", ranking.size());

            return ResponseEntity.ok(Map.of(
                    "ranking",        ranking,
                    "resumenGeneral", resumenGeneral != null ? resumenGeneral : ""
            ));

        } catch (RuntimeException e) {
            log.error("[IA-RANKING] Error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}