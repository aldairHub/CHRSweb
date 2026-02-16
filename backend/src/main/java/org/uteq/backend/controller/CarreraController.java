package org.uteq.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import org.uteq.backend.service.CarreraService;
import org.uteq.backend.dto.CarreraRequestDTO;
import org.uteq.backend.dto.CarreraResponseDTO;

@RestController
@RequestMapping("/api/carreras")
@CrossOrigin(origins = "*")
public class CarreraController {

    private final CarreraService carreraService;

    public CarreraController(CarreraService carreraService) {
        this.carreraService = carreraService;
    }

    @PostMapping
    public CarreraResponseDTO crear(@RequestBody CarreraRequestDTO dto) {
        return carreraService.crear(dto);
    }

    @GetMapping
    public List<CarreraResponseDTO> listar() {
        return carreraService.listar();
    }

    @GetMapping("/{id}")
    public CarreraResponseDTO obtener(@PathVariable Long id) {
        return carreraService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public CarreraResponseDTO actualizar(
            @PathVariable Long id,
            @RequestBody CarreraRequestDTO dto) {
        return carreraService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        carreraService.eliminar(id);
    }
}
