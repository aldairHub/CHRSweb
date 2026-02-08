package org.uteq.backend.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.uteq.backend.Service.AutoridadAcademicaService;
import org.uteq.backend.dto.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.AutoridadAcademicaResponseDTO;
import org.uteq.backend.dto.AutoridadRegistroRequestDTO;

@RestController
@RequestMapping("/api/autoridades-academicas")
@CrossOrigin
public class AutoridadAcademicaController {

    private final AutoridadAcademicaService autoridadService;

    public AutoridadAcademicaController(AutoridadAcademicaService autoridadService) {
        this.autoridadService = autoridadService;
    }

    @PostMapping
    public AutoridadAcademicaResponseDTO crear(@RequestBody AutoridadAcademicaRequestDTO dto) {
        return autoridadService.crear(dto);
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody AutoridadRegistroRequestDTO dto) {
        return ResponseEntity.ok(autoridadService.registrarAutoridad(dto));
    }

    @GetMapping
    public List<AutoridadAcademicaResponseDTO> listar() {
        return autoridadService.listar();
    }

    @GetMapping("/{id}")
    public AutoridadAcademicaResponseDTO obtener(@PathVariable Long id) {
        return autoridadService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public AutoridadAcademicaResponseDTO actualizar(
            @PathVariable Long id,
            @RequestBody AutoridadAcademicaRequestDTO dto) {
        return autoridadService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        autoridadService.eliminar(id);
    }
}
