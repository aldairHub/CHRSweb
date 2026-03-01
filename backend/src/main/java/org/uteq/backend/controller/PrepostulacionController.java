package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.service.PrepostulacionService;
import org.uteq.backend.dto.PrepostulacionResponseDTO;

@RestController
@RequestMapping("/api/prepostulacion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PrepostulacionController {

    private final PrepostulacionService prepostulacionService;

    /**
     * Registro inicial de postulante.
     * Recibe idSolicitud (no idConvocatoria) — el postulante ya eligió la solicitud específica en el front.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registrarPrepostulacion(
            @RequestParam("correo")                  String correo,
            @RequestParam("cedula")                  String cedula,
            @RequestParam("nombres")                 String nombres,
            @RequestParam("apellidos")               String apellidos,
            @RequestParam("archivoCedula")           MultipartFile archivoCedula,
            @RequestParam("archivoFoto")             MultipartFile archivoFoto,
            @RequestParam("archivoPrerrequisitos")   MultipartFile archivoPrerrequisitos,
            @RequestParam(value = "idSolicitud", required = false) Long idSolicitud
    ) {
        try {
            PrepostulacionResponseDTO response = prepostulacionService.procesarPrepostulacion(
                    correo, cedula, nombres, apellidos,
                    archivoCedula, archivoFoto, archivoPrerrequisitos,
                    idSolicitud
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PrepostulacionResponseDTO(
                    e.getMessage(), correo, null, false, null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new PrepostulacionResponseDTO(
                    "Error interno del servidor: " + e.getMessage(), correo, null, false, null
            ));
        }
    }

    /**
     * Verifica si una cédula está disponible (no ha postulado nunca).
     */
    @GetMapping("/verificar-cedula/{cedula}")
    public ResponseEntity<?> verificarCedula(@PathVariable String cedula) {
        try {
            return ResponseEntity.ok().body(java.util.Map.of(
                    "disponible", true,
                    "mensaje", "Cédula disponible"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "disponible", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * Verifica el estado de la última postulación de una cédula.
     * Usado por el componente de re-postulación.
     */
    @GetMapping("/verificar-estado/{cedula}")
    public ResponseEntity<?> verificarEstado(@PathVariable String cedula) {
        try {
            String estado = prepostulacionService.obtenerEstadoPorCedula(cedula);
            return ResponseEntity.ok().body(java.util.Map.of(
                    "encontrado", true,
                    "estado", estado
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(java.util.Map.of(
                    "encontrado", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * Re-postulación: solo para postulantes con estado RECHAZADO.
     * Guarda una NUEVA fila en prepostulacion (historial) via stored procedure.
     * Recibe idSolicitud (el postulante elige de nuevo a cuál solicitud aplica).
     */
    @PostMapping(value = "/repostular", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> repostular(
            @RequestParam("cedula")                  String cedula,
            @RequestParam("archivoCedula")           MultipartFile archivoCedula,
            @RequestParam("archivoFoto")             MultipartFile archivoFoto,
            @RequestParam("archivoPrerrequisitos")   MultipartFile archivoPrerrequisitos,
            @RequestParam(value = "idSolicitud", required = false) Long idSolicitud
    ) {
        try {
            PrepostulacionResponseDTO response = prepostulacionService.repostular(
                    cedula, archivoCedula, archivoFoto, archivoPrerrequisitos, idSolicitud
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("mensaje", "Error interno: " + e.getMessage()));
        }
    }
}