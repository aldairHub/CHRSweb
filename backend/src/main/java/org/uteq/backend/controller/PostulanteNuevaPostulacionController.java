package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.dto.PrepostulacionResponseDTO;
import org.uteq.backend.service.JwtService;
import org.uteq.backend.service.NuevaPostulacionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/postulante/nueva-postulacion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PostulanteNuevaPostulacionController {

    private final NuevaPostulacionService nuevaPostulacionService;
    private final JwtService              jwtService;

    /** GET /api/postulante/nueva-postulacion/mis-documentos */
    @GetMapping("/mis-documentos")
    public ResponseEntity<?> misDocumentos(HttpServletRequest request) {
        try {
            String usuarioApp = extraerUsuario(request);
            return ResponseEntity.ok(nuevaPostulacionService.getMisDocumentos(usuarioApp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    /** GET /api/postulante/nueva-postulacion/verificar/{idSolicitud} */
    @GetMapping("/verificar/{idSolicitud}")
    public ResponseEntity<?> verificar(@PathVariable Long idSolicitud, HttpServletRequest request) {
        try {
            String usuarioApp = extraerUsuario(request);
            boolean yaPostulo = nuevaPostulacionService.yaPostuloASolicitud(usuarioApp, idSolicitud);
            return ResponseEntity.ok(Map.of("yaPostulo", yaPostulo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    /** POST /api/postulante/nueva-postulacion */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> postular(
            @RequestParam("idSolicitud")                                            Long idSolicitud,
            @RequestParam(value = "archivosDocumentos",       required = false)     List<MultipartFile> archivosDocumentos,
            @RequestParam(value = "descripcionesDocumentos",  required = false)     List<String> descripcionesDocumentos,
            @RequestParam(value = "archivosRequisitos",       required = false)     List<MultipartFile> archivosRequisitos,
            @RequestParam(value = "idsRequisitos",            required = false)     List<Long> idsRequisitos,
            @RequestParam(value = "nombresRequisitos",        required = false)     List<String> nombresRequisitos,
            @RequestParam(value = "idsDocumentosReutilizados",required = false)     List<Long> idsDocumentosReutilizados,
            @RequestParam(value = "urlCedulaReutilizada",     required = false)     String urlCedulaReutilizada,
            @RequestParam(value = "urlFotoReutilizada",       required = false)     String urlFotoReutilizada,
            @RequestParam(value = "archivoCedulaNueva",       required = false)     MultipartFile archivoCedulaNueva,
            @RequestParam(value = "archivoFotoNueva",         required = false)     MultipartFile archivoFotoNueva,
            HttpServletRequest request
    ) {
        try {
            String usuarioApp = extraerUsuario(request);
            PrepostulacionResponseDTO response = nuevaPostulacionService.postular(
                    usuarioApp, idSolicitud,
                    archivosDocumentos, descripcionesDocumentos,
                    archivosRequisitos, idsRequisitos, nombresRequisitos,
                    idsDocumentosReutilizados,
                    urlCedulaReutilizada, urlFotoReutilizada,
                    archivoCedulaNueva, archivoFotoNueva
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error interno: " + e.getMessage()));
        }
    }

    private String extraerUsuario(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new RuntimeException("Token requerido");
        return jwtService.extractUsername(authHeader.substring(7));
    }
}