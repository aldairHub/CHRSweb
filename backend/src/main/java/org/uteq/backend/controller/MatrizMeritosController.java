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

    @GetMapping("/solicitud/{idSolicitud}")
    public ResponseEntity<?> obtenerMatriz(@PathVariable Long idSolicitud) {
        return ResponseEntity.ok(service.obtenerMatrizPorSolicitud(idSolicitud));
    }

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