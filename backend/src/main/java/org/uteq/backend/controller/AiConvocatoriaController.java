package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.GenerarDescripcionRequestDTO;
import org.uteq.backend.entity.SolicitudDocente;
import org.uteq.backend.repository.SolicitudDocenteRepository;
import org.uteq.backend.service.AiTextService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/revisor/convocatorias")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiConvocatoriaController {

    private final AiTextService              aiTextService;
    private final SolicitudDocenteRepository solicitudRepo;

    @PostMapping("/generar-descripcion")
    public ResponseEntity<Map<String, String>> generarDescripcion(
            @RequestBody GenerarDescripcionRequestDTO req
    ) {
        // Contexto enriquecido: materia + justificación por cada solicitud
        List<AiTextService.SolicitudContexto> contextos = new ArrayList<>();

        if (req.getIdsSolicitudes() != null && !req.getIdsSolicitudes().isEmpty()) {
            for (Long id : req.getIdsSolicitudes()) {
                solicitudRepo.findById(id).ifPresent(s -> {
                    String materia       = s.getMateria() != null ? s.getMateria().getNombreMateria() : null;
                    String justificacion = s.getJustificacion();
                    if (justificacion != null && !justificacion.isBlank()) {
                        contextos.add(new AiTextService.SolicitudContexto(materia, justificacion));
                    }
                });
            }
        }

        // Fallback: el frontend envió justificaciones directamente (sin IDs)
        if (contextos.isEmpty() && req.getJustificaciones() != null) {
            req.getJustificaciones().forEach(j ->
                    contextos.add(new AiTextService.SolicitudContexto(null, j))
            );
        }

        if (contextos.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se encontraron justificaciones para generar la descripción."));
        }

        String descripcion = aiTextService.generateConvocatoriaDescripcion(contextos);
        return ResponseEntity.ok(Map.of("descripcion", descripcion));
    }
}