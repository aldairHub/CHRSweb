package org.uteq.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.impl.SolicitudDocenteService;
import org.uteq.backend.dto.SolicitudDocenteRequestDTO;
import org.uteq.backend.dto.SolicitudDocenteResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes-docente")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SolicitudDocenteController {

    private final SolicitudDocenteService solicitudService;

    // =====================================================
    // 📦 DTOs INTERNOS (DEBEN IR PRIMERO)
    // =====================================================
    public static class SolicitudConUsuarioDTO {
        private String usuarioApp;
        private SolicitudDocenteRequestDTO solicitud;

        public String getUsuarioApp() { return usuarioApp; }
        public void setUsuarioApp(String usuarioApp) { this.usuarioApp = usuarioApp; }

        public SolicitudDocenteRequestDTO getSolicitud() { return solicitud; }
        public void setSolicitud(SolicitudDocenteRequestDTO solicitud) { this.solicitud = solicitud; }
    }

    public record ErrorResponse(String mensaje, int status) {}

    // =====================================================
    // ⭐ CREAR SOLICITUD
    // =====================================================
    @PostMapping
    public ResponseEntity<?> crearSolicitud(@Valid @RequestBody SolicitudConUsuarioDTO request) {
        try {
            Long idAutoridad = solicitudService.obtenerIdAutoridadPorUsuarioApp(request.getUsuarioApp());
            SolicitudDocenteResponseDTO response =
                    solicitudService.crearSolicitud(request.getSolicitud(), idAutoridad);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    // =====================================================
    // 📥 LISTAR TODAS
    // =====================================================
    @GetMapping
    public ResponseEntity<List<SolicitudDocenteResponseDTO>> obtenerTodasLasSolicitudes() {
        return ResponseEntity.ok(solicitudService.obtenerTodasLasSolicitudes());
    }

    // =====================================================
    // 🔍 POR ID
    // =====================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerSolicitudPorId(@PathVariable Long id) {
        try {
            SolicitudDocenteResponseDTO solicitud = solicitudService.obtenerSolicitudPorId(id);
            return ResponseEntity.ok(solicitud);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No se encontró la solicitud con ID " + id, HttpStatus.NOT_FOUND.value()));
        }
    }

    // =====================================================
    // 🔥 PDF MEJORADO CON LOGGING DE ERRORES
    // =====================================================
    @GetMapping("/{id}/reporte-pdf")
    public ResponseEntity<?> generarReportePdf(@PathVariable Long id) {
        try {
            System.out.println("🔍 Generando PDF para solicitud ID: " + id);

            byte[] pdf = solicitudService.generarPDFSolicitud(id);

            if (pdf == null || pdf.length == 0) {
                System.err.println("❌ El PDF generado está vacío");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("El PDF generado está vacío", 500));
            }

            System.out.println("✅ PDF generado correctamente. Tamaño: " + pdf.length + " bytes");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=solicitud_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            // ✅ AHORA SÍ MOSTRAMOS EL ERROR COMPLETO
            System.err.println("❌ ERROR GENERANDO PDF:");
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(
                            "Error generando PDF: " + e.getMessage() +
                                    " | Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "desconocida"),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
    }

    // =====================================================
    // ⚠️ MANEJO DE ERRORES
    // =====================================================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace(); // Para debugging
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}