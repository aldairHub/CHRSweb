package org.uteq.backend.Controller;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.uteq.backend.dto.AreaConocimientoRequestDTO;
import org.uteq.backend.dto.AreaConocimientoResponseDTO;
import org.uteq.backend.Service.AreaConocimientoService;

@RestController
@RequestMapping("/api/areas-conocimiento")
public class AreaConocimientoController {

    private final AreaConocimientoService service;

    public AreaConocimientoController(AreaConocimientoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AreaConocimientoResponseDTO> crear(
            @Valid @RequestBody AreaConocimientoRequestDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @GetMapping
    public ResponseEntity<List<AreaConocimientoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaConocimientoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaConocimientoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AreaConocimientoRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
