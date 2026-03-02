package org.uteq.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.service.InstitucionService;
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

    @GetMapping("/activa")
    public InstitucionResponseDTO obtenerActiva() {
        return institucionService.obtenerActiva();
    }
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String url = institucionService.uploadLogo(id, file);
        return ResponseEntity.ok(Map.of("logoUrl", url));
    }

}
