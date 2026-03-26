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
import org.uteq.backend.service.ReporteMatrizService;
@RestController
@RequestMapping("/api/instituciones")
@CrossOrigin
public class InstitucionController {

    private final InstitucionService institucionService;
    private final ReporteMatrizService reporteService;

    public InstitucionController(InstitucionService institucionService, ReporteMatrizService reporteService) {
        this.institucionService = institucionService;
        this.reporteService = reporteService;
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
    @PostMapping(value = "/{id}/escudo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadEscudo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String url = institucionService.uploadEscudo(id, file);
        return ResponseEntity.ok(Map.of("escudoUrl", url));
    }

    // ── Endpoints de preview PDF (solo para desarrollo) ──
    @GetMapping("/preview/acta-pdf")
    public ResponseEntity<byte[]> previewActa() {
        byte[] pdf = reporteService.generarPreviewActa();
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"preview-acta.pdf\"")
                .body(pdf);
    }

    @GetMapping("/preview/informe-pdf")
    public ResponseEntity<byte[]> previewInforme() {
        byte[] pdf = reporteService.generarPreviewInforme();
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"preview-informe.pdf\"")
                .body(pdf);
    }
}
