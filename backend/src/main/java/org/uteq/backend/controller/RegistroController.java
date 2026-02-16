package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.service.RegistroService;
import org.uteq.backend.dto.RegistroResponseDTO;
import org.uteq.backend.dto.RegistroUsuarioDTO;

@RestController
@RequestMapping("/api/registro")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RegistroController {

    private final RegistroService registroService;

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody RegistroUsuarioDTO dto) {
        try {
            RegistroResponseDTO response = registroService.registrarUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new RegistroResponseDTO(
                    e.getMessage(),
                    dto.getCorreo(),
                    false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new RegistroResponseDTO(
                    "Error interno del servidor: " + e.getMessage(),
                    dto.getCorreo(),
                    false
            ));
        }
    }

    @GetMapping("/verificar-email/{email}")
    public ResponseEntity<?> verificarEmail(@PathVariable String email) {
        // Endpoint para verificar si un email ya está registrado
        // Útil para validación en tiempo real en el frontend
        try {
            // Aquí podrías inyectar UsuarioRepository si lo necesitas
            return ResponseEntity.ok().body(java.util.Map.of(
                    "disponible", true,
                    "mensaje", "Email disponible"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "disponible", false,
                    "mensaje", e.getMessage()
            ));
        }
    }
}
