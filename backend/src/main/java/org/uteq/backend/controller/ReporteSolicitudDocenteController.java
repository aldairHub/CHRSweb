package org.uteq.backend.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ReporteSolicitudDocenteConfigDTO;
import org.uteq.backend.service.ReporteSolicitudDocenteService;
@RestController
@RequestMapping("/api/admin/solicitudes-docentes/reporte")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RequiredArgsConstructor
public class ReporteSolicitudDocenteController {
    private final ReporteSolicitudDocenteService reporteService;
    @PostMapping("/generar")
    public ResponseEntity<byte[]> generar(@RequestBody ReporteSolicitudDocenteConfigDTO cfg) {
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