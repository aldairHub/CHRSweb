package org.uteq.backend.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import org.uteq.backend.Service.InstitucionService;
import org.uteq.backend.dto.InstitucionRequestDTO;
import org.uteq.backend.dto.InstitucionResponseDTO;

@RestController
@RequestMapping("/api/instituciones")
@CrossOrigin
public class InstitucionController {

    private final InstitucionService institucionService;

    public InstitucionController(InstitucionService institucionService) {
        this.institucionService = institucionService;
    }

    @PostMapping
    public InstitucionResponseDTO crear(@RequestBody InstitucionRequestDTO dto) {
        return institucionService.crear(dto);
    }

    @GetMapping
    public List<InstitucionResponseDTO> listar() {
        return institucionService.listar();
    }

    @GetMapping("/{id}")
    public InstitucionResponseDTO obtener(@PathVariable Long id) {
        return institucionService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public InstitucionResponseDTO actualizar(
            @PathVariable Long id,
            @RequestBody InstitucionRequestDTO dto) {
        return institucionService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        institucionService.eliminar(id);
    }
}
