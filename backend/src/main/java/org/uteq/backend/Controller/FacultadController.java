package org.uteq.backend.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import org.uteq.backend.Service.FacultadService;
import org.uteq.backend.dto.FacultadRequestDTO;
import org.uteq.backend.dto.FacultadResponseDTO;

@RestController
@RequestMapping("/api/facultades")
@CrossOrigin
public class FacultadController {

    private final FacultadService facultadService;

    public FacultadController(FacultadService facultadService) {
        this.facultadService = facultadService;
    }

    @PostMapping
    public FacultadResponseDTO crear(@RequestBody FacultadRequestDTO dto) {
        return facultadService.crear(dto);
    }

    @GetMapping
    public List<FacultadResponseDTO> listar() {
        return facultadService.listar();
    }

    @GetMapping("/{id}")
    public FacultadResponseDTO obtener(@PathVariable Long id) {
        return facultadService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public FacultadResponseDTO actualizar(
            @PathVariable Long id,
            @RequestBody FacultadRequestDTO dto) {
        return facultadService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        facultadService.eliminar(id);
    }
}
