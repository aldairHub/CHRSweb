package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.CambiarClaveDTO;
import org.uteq.backend.dto.UsuarioCreateDTO;
import org.uteq.backend.dto.UsuarioDTO;
import org.uteq.backend.dto.UsuarioUpdateDTO;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.AesCipherService;
import org.uteq.backend.service.JwtService;
import org.uteq.backend.service.UsuarioService;

import java.security.SecureRandom;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {


    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    public UsuarioController(UsuarioService usuarioService,
                             JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    // Crear usuario (solo ADMIN o EVALUATOR)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioCreateDTO dto) {
        try {
            UsuarioDTO usuario = usuarioService.crear(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Listar todos
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }


    // Obtener por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            UsuarioDTO usuario = usuarioService.obtenerPorId(id);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    // Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody UsuarioUpdateDTO dto) {
        try {
            UsuarioDTO usuario = usuarioService.actualizar(id, dto);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Activar/Desactivar
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        try {
            UsuarioDTO usuario = usuarioService.cambiarEstado(id, activo);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
//    // ✅ TEMPORAL - Eliminar después de usar UNA SOLA VEZ
//    @GetMapping("/migrar-claves-bd")
//    public ResponseEntity<String> migrarClavesBd() {
//        List<Usuario> usuarios = usuarioRepository.findAll();
//        int migrados = 0;
//        int errores = 0;
//
//        for (Usuario u : usuarios) {
//            try {
//                String claveActual = u.getClaveBd();
//                String claveEnTextoPlano;
//
//                // ✅ Verificar si es Base64 válido antes de decodificar
//                if (esBase64Valido(claveActual)) {
//                    claveEnTextoPlano = new String(java.util.Base64.getDecoder().decode(claveActual));
//                    System.out.println("📦 " + u.getUsuarioApp() + " → era Base64 → " + claveEnTextoPlano);
//                } else {
//                    // Ya está en texto plano
//                    claveEnTextoPlano = claveActual;
//                    System.out.println("📝 " + u.getUsuarioApp() + " → era texto plano → " + claveEnTextoPlano);
//                }
//
//                // Cifrar con AES
//                String claveCifrada = aesCipherService.cifrar(claveEnTextoPlano);
//                u.setClaveBd(claveCifrada);
//                usuarioRepository.saveAndFlush(u);
//                migrados++;
//                System.out.println("✅ Migrado: " + u.getUsuarioApp());
//
//            } catch (Exception e) {
//                errores++;
//                System.err.println("❌ Error migrando " + u.getUsuarioApp() + ": " + e.getMessage());
//            }
//        }
//
//        return ResponseEntity.ok(
//                "Migración completa. Migrados: " + migrados + " | Errores: " + errores
//        );
//    }
//
//    // Método helper para verificar si un string es Base64 válido
//    private boolean esBase64Valido(String valor) {
//        if (valor == null || valor.isBlank()) return false;
//        // Base64 válido solo contiene estos caracteres y su longitud es múltiplo de 4
//        return valor.matches("^[A-Za-z0-9+/]*={0,2}$") && valor.length() % 4 == 0;
//    }
    // ─── Caso 1: Primer login ───────────────────────────────────

    @PutMapping("/primer-login/cambiar-clave")
    public ResponseEntity<?> cambiarClavePrimerLogin(
            @RequestBody CambiarClaveDTO dto,
            HttpServletRequest request) {
        try {
            String usuarioApp = extraerUsuarioApp(request);
            usuarioService.cambiarClavePrimerLogin(dto);
            return ResponseEntity.ok("Contraseña actualizada. Ya puedes acceder al sistema.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── Caso 2: Cambio voluntario ─────────────────────────────

    @PutMapping("/cambiar-clave")
    public ResponseEntity<?> cambiarClave(
            @RequestBody CambiarClaveDTO dto,
            HttpServletRequest request) {
        try {
            String usuarioApp = extraerUsuarioApp(request);
            usuarioService.cambiarClave(usuarioApp, dto);
            return ResponseEntity.ok("Contraseña actualizada correctamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── Caso 3: Olvidó contraseña ─────────────────────────────

    @PostMapping("/recuperar-clave")
    public ResponseEntity<?> recuperarClave(@RequestParam String correo) {
        // Mismo mensaje siempre — no revelar si el correo existe o no
        try {
            usuarioService.recuperarClave(correo);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(
                "Si el correo está registrado, recibirás las instrucciones en breve."
        );
    }

    // ─── Helper ────────────────────────────────────────────────

    private String extraerUsuarioApp(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Token no proporcionado");
        }
        return jwtService.extractUsername(header.substring(7));
    }

}