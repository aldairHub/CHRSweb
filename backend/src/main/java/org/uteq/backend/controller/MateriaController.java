package org.uteq.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.MateriaService;
import org.uteq.backend.dto.MateriaRequestDTO;
import org.uteq.backend.dto.MateriaResponseDTO;

@RestController
@RequestMapping("/api/materias")
@CrossOrigin
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @PostMapping
    public MateriaResponseDTO crear(@RequestBody MateriaRequestDTO dto) {
        return materiaService.crear(dto);
    }

    @PutMapping("/{id}")
    public MateriaResponseDTO actualizar(@PathVariable Long id,
                                         @RequestBody MateriaRequestDTO dto) {
        return materiaService.actualizar(id, dto);
    }


    @GetMapping
    public List<MateriaResponseDTO> listar() {
        return materiaService.listar();
    }

    @GetMapping("/carrera/{idCarrera}")
    public List<MateriaResponseDTO> listarPorCarrera(@PathVariable Long idCarrera) {
        return materiaService.listarPorCarrera(idCarrera);
    }
}
