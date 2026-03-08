package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.MatrizMeritosService;

import java.util.Map;

@RestController
@RequestMapping("/api/matriz-meritos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MatrizMeritosController {

    private final MatrizMeritosService service;

    /**
     * GET /api/matriz-meritos/convocatoria/{idConvocatoria}
     * Devuelve info de la convocatoria + candidatos con sus puntajes actuales
     */
    @GetMapping("/convocatoria/{idConvocatoria}")
    public ResponseEntity<Map<String, Object>> obtenerMatriz(
            @PathVariable Long idConvocatoria) {
        return ResponseEntity.ok(service.obtenerMatriz(idConvocatoria));
    }

    /**
     * POST /api/matriz-meritos/guardar
     * Guarda los puntajes de todos los candidatos
     */
    @PostMapping("/guardar")
    public ResponseEntity<Map<String, Object>> guardar(
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(service.guardarPuntajes(payload));
    }

    @GetMapping("/convocatorias")
    public ResponseEntity<?> listarConvocatorias() {
        return ResponseEntity.ok(service.listarConvocatorias());
    }
}
