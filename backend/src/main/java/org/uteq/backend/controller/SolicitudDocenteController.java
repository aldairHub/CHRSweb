package org.uteq.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.ConvocatoriaAdminService;
import org.uteq.backend.service.SolicitudDocenteService;
import org.uteq.backend.dto.SolicitudDocenteRequestDTO;
import org.uteq.backend.dto.SolicitudDocenteResponseDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes-docente")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SolicitudDocenteController {

    private final SolicitudDocenteService solicitudService;
    private final ConvocatoriaAdminService convocatoriaAdminService;

    // =====================================================
    // DTOs INTERNOS
    // =====================================================
    public static class SolicitudConUsuarioDTO {
        private String usuarioApp;
        private SolicitudDocenteRequestDTO solicitud;

        public String getUsuarioApp() { return usuarioApp; }
        public void setUsuarioApp(String usuarioApp) { this.usuarioApp = usuarioApp; }
        public SolicitudDocenteRequestDTO getSolicitud() { return solicitud; }
        public void setSolicitud(SolicitudDocenteRequestDTO solicitud) { this.solicitud = solicitud; }
    }

    public static class CambiarEstadoRequest {
        private String nuevoEstado;
        private String observaciones;

        public String getNuevoEstado() { return nuevoEstado; }
        public void setNuevoEstado(String nuevoEstado) { this.nuevoEstado = nuevoEstado; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    }

    public record ErrorResponse(String mensaje, int status) {}

    // =====================================================
    // CREAR SOLICITUD
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
    // LISTAR TODAS
    // =====================================================
    @GetMapping
    public ResponseEntity<List<SolicitudDocenteResponseDTO>> obtenerTodasLasSolicitudes() {
        return ResponseEntity.ok(solicitudService.obtenerTodasLasSolicitudes());
    }

    // =====================================================
    // FILTRAR POR ESTADO  ← NUEVO
    // GET /api/solicitudes-docente/estado/aprobada
    // =====================================================
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<SolicitudDocenteResponseDTO>> obtenerPorEstado(
            @PathVariable String estado) {
        return ResponseEntity.ok(solicitudService.obtenerPorEstado(estado));
    }
    // GET /api/solicitudes-docente/disponibles-para-convocatoria
    // Excluye solicitudes ya asignadas a convocatorias activas
    // =====================================================
    @GetMapping("/disponibles-para-convocatoria")
    public ResponseEntity<List<SolicitudDocenteResponseDTO>> obtenerDisponiblesParaConvocatoria() {
        return ResponseEntity.ok(
                convocatoriaAdminService.getSolicitudesDisponiblesParaConvocatoria()
        );
    }
    // =====================================================
    // POR ID
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
    // CAMBIAR ESTADO (aprobar / rechazar)  ← NUEVO
    // PATCH /api/solicitudes-docente/{id}/estado
    // =====================================================
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambiarEstadoRequest request) {
        try {
            SolicitudDocenteResponseDTO response =
                    solicitudService.cambiarEstado(id, request.getNuevoEstado(), request.getObservaciones());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    // =====================================================
    // PDF
    // =====================================================
    @GetMapping("/{id}/reporte-pdf")
    public ResponseEntity<?> generarReportePdf(@PathVariable Long id) {
        try {
            System.out.println("🔍 Generando PDF para solicitud ID: " + id);
            byte[] pdf = solicitudService.generarPDFSolicitud(id);

            if (pdf == null || pdf.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("El PDF generado está vacío", 500));
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=solicitud_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
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
    // MANEJO DE ERRORES
    // =====================================================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace();
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()),
                HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/mi-facultad")
    public ResponseEntity<?> obtenerMiFacultad(@RequestParam String usuarioApp) {
        try {
            Long idFacultad = solicitudService.obtenerIdFacultadPorUsuarioApp(usuarioApp);
            return ResponseEntity.ok(Map.of("idFacultad", idFacultad));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage(), 400));
        }
    }
}