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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registrarPrepostulacion(
            @RequestParam("correo") String correo,
            @RequestParam("cedula") String cedula,
            @RequestParam("nombres") String nombres,
            @RequestParam("apellidos") String apellidos,
            @RequestParam("archivoCedula") MultipartFile archivoCedula,
            @RequestParam("archivoFoto") MultipartFile archivoFoto,
            @RequestParam("archivoPrerrequisitos") MultipartFile archivoPrerrequisitos,
            @RequestParam(value = "idConvocatoria", required = false) Long idConvocatoria
    ) {
        try {
            PrepostulacionResponseDTO response = prepostulacionService.procesarPrepostulacion(
                    correo, cedula, nombres, apellidos,
                    archivoCedula, archivoFoto, archivoPrerrequisitos, idConvocatoria
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PrepostulacionResponseDTO(
                    e.getMessage(),
                    correo,
                    null,
                    false,
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new PrepostulacionResponseDTO(
                    "Error interno del servidor: " + e.getMessage(),
                    correo,
                    null,
                    false,
                    null
            ));
        }
    }

    @GetMapping("/verificar-cedula/{cedula}")
    public ResponseEntity<?> verificarCedula(@PathVariable String cedula) {
        // Este endpoint puede usarse para validar si una cédula ya está registrada
        // Útil para validación en tiempo real en el frontend
        try {
            // Aquí puedes agregar lógica adicional si lo necesitas
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
}
