package org.uteq.backend.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.Service.CodigoVerificacionService;
import org.uteq.backend.Service.EmailService;

@RestController
@RequestMapping("/api/verificacion")
@CrossOrigin
@RequiredArgsConstructor
public class VerificacionController {
    private final CodigoVerificacionService codigoService;
    private final EmailService emailService;

    // Paso 1: enviar código
    @PostMapping("/enviar")
    public ResponseEntity<?> enviarCodigo(
            @RequestParam String correo) {
        try
        {
            String codigo = codigoService.generarCodigo(correo);
            emailService.enviarCodigoVerificacion(correo, codigo);

            return ResponseEntity.ok().build();
        }
         catch (Exception e) {
            return ResponseEntity.status(500).body(
                    "No se pudo enviar el correo de verificación: " + e.getMessage()
            );
        }
    }

    // Paso 2: validar código
    @PostMapping("/validar")
    public ResponseEntity<Boolean> validarCodigo(
            @RequestParam String correo,
            @RequestParam String codigo) {

        boolean valido =
                codigoService.validarCodigo(correo, codigo);

        return ResponseEntity.ok(valido);
    }
}
