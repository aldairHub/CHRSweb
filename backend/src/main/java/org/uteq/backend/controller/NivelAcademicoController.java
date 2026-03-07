package org.uteq.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.NivelAcademicoRequestDTO;
import org.uteq.backend.dto.NivelAcademicoResponseDTO;
import org.uteq.backend.service.NivelAcademicoService;

import java.util.List;

@RestController
@RequestMapping("/api/niveles-academicos")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class NivelAcademicoController {

    private final NivelAcademicoService service;

    /** GET /api/niveles-academicos — todos (admin) */
    @GetMapping
    public List<NivelAcademicoResponseDTO> listar() {
        return service.listar();
    }

    /** GET /api/niveles-academicos/activos — solo activos (para selects) */
    @GetMapping("/activos")
    public List<NivelAcademicoResponseDTO> listarActivos() {
        return service.listarActivos();
    }

    /** POST /api/niveles-academicos */
    @PostMapping
    public ResponseEntity<NivelAcademicoResponseDTO> crear(@Valid @RequestBody NivelAcademicoRequestDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    /** PUT /api/niveles-academicos/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<NivelAcademicoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody NivelAcademicoRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    /** DELETE /api/niveles-academicos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}