package org.uteq.backend.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ReporteConvocatoriaConfigDTO;
import org.uteq.backend.service.ReporteConvocatoriaService;
@RestController
@RequestMapping("/api/admin/convocatorias/reporte")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RequiredArgsConstructor
public class ReporteConvocatoriaController {
    private final ReporteConvocatoriaService reporteService;
    @PostMapping("/generar")
    public ResponseEntity<byte[]> generar(@RequestBody ReporteConvocatoriaConfigDTO cfg) {
        byte[] archivo   = reporteService.generar(cfg);
        String nombre    = reporteService.nombreArchivo(cfg);
        String mediaType = "EXCEL".equalsIgnoreCase(cfg.getFormato())
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .contentLength(archivo.length).body(archivo);
    }
}