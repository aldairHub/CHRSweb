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
    /**
     * GET /api/matriz-config/tiene-procesos-activos
     * Retorna true si hay procesos activos que impidan editar la matriz
     */
    @GetMapping("/tiene-procesos-activos")
    public ResponseEntity<?> tieneProcesosActivos() {
        boolean tiene = service.tieneProcesosActivos();
        return ResponseEntity.ok(Map.of("tieneProcesosActivos", tiene));
    }

    /**
     * POST /api/matriz-config/confirmar-distribucion
     * Body: { "meritos": 50, "experiencia": 25, "entrevista": 25 }
     */
    @PostMapping("/confirmar-distribucion")
    public ResponseEntity<?> confirmarDistribucion(@RequestBody Map<String, Object> body) {
        Double meritos     = ((Number) body.get("meritos")).doubleValue();
        Double experiencia = ((Number) body.get("experiencia")).doubleValue();
        Double entrevista  = ((Number) body.get("entrevista")).doubleValue();

        if (meritos + experiencia + entrevista != 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "La distribución debe sumar exactamente 100 puntos."));
        }

        service.confirmarDistribucion(meritos, experiencia, entrevista);
        return ResponseEntity.ok(Map.of("mensaje", "Distribución confirmada correctamente."));
    }

}
