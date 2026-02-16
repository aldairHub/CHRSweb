package org.uteq.backend.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import org.uteq.backend.service.FacultadService;
import org.uteq.backend.dto.FacultadRequestDTO;
import org.uteq.backend.dto.FacultadResponseDTO;

@RestController
@RequestMapping("/api/facultades")
@CrossOrigin(origins = "http://localhost:4200")
public class FacultadController {

    private final FacultadService facultadService;

    public FacultadController(FacultadService facultadService) {
        this.facultadService = facultadService;
    }

    @PostMapping
    public FacultadResponseDTO crear(@Valid @RequestBody FacultadRequestDTO dto) {
        return facultadService.crear(dto);
    }

    @PutMapping("/{id}")
    public FacultadResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody FacultadRequestDTO dto) {
        return facultadService.actualizar(id, dto);
    }
    @GetMapping
    public List<FacultadResponseDTO> listar() {
        return facultadService.listar();
    }

    @GetMapping("/{id}")
    public FacultadResponseDTO obtener(@PathVariable Long id) {
        return facultadService.obtenerPorId(id);
    }


    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        facultadService.eliminar(id);
    }
}
