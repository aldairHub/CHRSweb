package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.MatrizConfigService;

import java.util.Map;

@RestController
@RequestMapping("/api/matriz-config")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MatrizConfigController {

    private final MatrizConfigService service;

    @GetMapping("/estructura")
    public ResponseEntity<?> obtenerEstructura() {
        return ResponseEntity.ok(service.obtenerEstructura());
    }

    @PostMapping("/seccion")
    public ResponseEntity<?> guardarSeccion(@RequestBody Map<String, Object> body) {
        service.guardarSeccion(body);
        return ResponseEntity.ok(Map.of("mensaje", "Sección guardada correctamente"));
    }

    @DeleteMapping("/seccion/{idSeccion}")
    public ResponseEntity<?> eliminarSeccion(@PathVariable Long idSeccion) {
        service.eliminarSeccion(idSeccion);
        return ResponseEntity.ok(Map.of("mensaje", "Sección eliminada correctamente"));
    }

    @PostMapping("/item")
    public ResponseEntity<?> guardarItem(@RequestBody Map<String, Object> body) {
        service.guardarItem(body);
        return ResponseEntity.ok(Map.of("mensaje", "Ítem guardado correctamente"));
    }

    @DeleteMapping("/item/{idItem}")
    public ResponseEntity<?> eliminarItem(@PathVariable Long idItem) {
        service.eliminarItem(idItem);
        return ResponseEntity.ok(Map.of("mensaje", "Ítem eliminado correctamente"));
    }
}
