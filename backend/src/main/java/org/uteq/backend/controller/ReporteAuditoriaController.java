package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ReporteAuditoriaConfigDTO;
import org.uteq.backend.service.ReporteAuditoriaService;

@RestController
@RequestMapping("/api/admin/auditoria/reporte")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReporteAuditoriaController {

    private final ReporteAuditoriaService reporteService;

    /**
     * POST /api/admin/auditoria/reporte/generar
     *
     * Recibe la configuración completa del reporte (secciones, filtros,
     * opciones visuales) y devuelve el archivo como descarga directa.
     * El formato (PDF/Excel) viene en el body del request.
     */
    @PostMapping("/generar")
    public ResponseEntity<byte[]> generar(@RequestBody ReporteAuditoriaConfigDTO cfg) {

        byte[] archivo   = reporteService.generar(cfg);
        String nombre    = reporteService.nombreArchivo(cfg);
        String mediaType = "EXCEL".equalsIgnoreCase(cfg.getFormato())
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .contentLength(archivo.length)
                .body(archivo);
    }
}