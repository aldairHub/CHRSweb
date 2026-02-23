package org.uteq.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.TipoDocumentoDTO;
import org.uteq.backend.service.TipoDocumentoService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tipos-documento")
@CrossOrigin(origins = "http://localhost:4200")
public class TipoDocumentoController {

    private final TipoDocumentoService service;

    public TipoDocumentoController(TipoDocumentoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TipoDocumentoDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = service.crear(
                (String) body.get("nombre"),
                (String) body.get("descripcion"),
                (Boolean) body.get("obligatorio")
        );
        String msg = (String) result.get("mensaje");
        return msg != null && msg.startsWith("ERROR")
                ? ResponseEntity.badRequest().body(result)
                : ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> editar(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = service.editar(
                id,
                (String) body.get("nombre"),
                (String) body.get("descripcion"),
                (Boolean) body.get("obligatorio")
        );
        boolean exitoso = Boolean.TRUE.equals(result.get("exitoso"));
        return exitoso ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long id) {
        Map<String, Object> result = service.toggle(id);
        boolean exitoso = Boolean.TRUE.equals(result.get("exitoso"));
        return exitoso ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }
}