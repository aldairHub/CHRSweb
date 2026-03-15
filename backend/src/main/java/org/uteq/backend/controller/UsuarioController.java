package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.dto.*;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService          usuarioService;
    private final JwtService              jwtService;
    private final UsuarioRepository       usuarioRepository;
    private final SupabaseStorageService  storageService;
    private final PostgresProcedureRepository procedureRepository;
    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    private static final long MAX_FOTO_BYTES = 2 * 1024 * 1024; // 2 MB

    public UsuarioController(UsuarioService usuarioService,
                             JwtService jwtService,
                             UsuarioRepository usuarioRepository,
                             SupabaseStorageService storageService,
                             PostgresProcedureRepository procedureRepository) {
        this.usuarioService      = usuarioService;
        this.jwtService          = jwtService;
        this.usuarioRepository   = usuarioRepository;
        this.storageService      = storageService;
        this.procedureRepository = procedureRepository;
    }

    // ─── Crear usuario (solo ADMIN o EVALUATOR) ────────────────────────────
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioCreateDTO dto) {
        try {
            UsuarioDTO usuario = usuarioService.crear(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ─── Listar todos ──────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // ─── Obtener por ID ────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            UsuarioDTO usuario = usuarioService.obtenerPorId(id);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    // ─── Actualizar ────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody UsuarioUpdateDTO dto) {
        try {
            UsuarioDTO usuario = usuarioService.actualizar(id, dto);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ─── Activar/Desactivar ────────────────────────────────────────────────
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        try {
            UsuarioDTO usuario = usuarioService.cambiarEstado(id, activo);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ─── Caso 1: Primer login ──────────────────────────────────────────────
    @PutMapping("/primer-login/cambiar-clave")
    public ResponseEntity<?> cambiarClavePrimerLogin(
            @RequestBody CambiarClaveDTO dto,
            HttpServletRequest request) {
        try {
            String usuarioApp = extraerUsuarioApp(request);
            usuarioService.cambiarClavePrimerLogin(usuarioApp, dto);
            return ResponseEntity.ok("Contraseña actualizada. Ya puedes acceder al sistema.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── Caso 2: Cambio voluntario ────────────────────────────────────────
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

    // ─── Caso 3: Olvidó contraseña ────────────────────────────────────────
    @PostMapping("/recuperar-clave")
    public ResponseEntity<?> recuperarClave(@RequestParam String correo) {
        try { usuarioService.recuperarClave(correo); } catch (Exception ignored) {}
        return ResponseEntity.ok(
                "Si el correo está registrado, recibirás las instrucciones en breve.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NUEVOS ENDPOINTS — Perfil y foto de perfil
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/usuarios/mi-perfil
     * Devuelve { usuarioApp, correo, fotoPerfil } del usuario autenticado.
     */
    @GetMapping("/mi-perfil")
    public ResponseEntity<?> miPerfil(HttpServletRequest request) {
        try {
            String usuarioApp = extraerUsuarioApp(request);
            Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            PerfilUsuarioDTO dto = new PerfilUsuarioDTO(
                    usuario.getUsuarioApp(),
                    usuario.getCorreo(),
                    usuario.getFotoPerfil()
            );
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/usuarios/foto-perfil
     * Recibe MultipartFile (imagen), la sube a Supabase Storage bucket "fotos-perfil"
     * y guarda la URL pública en usuario.foto_perfil_url.
     */
    @PutMapping(value = "/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirFotoPerfil(
            @RequestParam("foto") MultipartFile foto,
            HttpServletRequest request) {
        try {
            // Validaciones
            if (foto == null || foto.isEmpty()) {
                return ResponseEntity.badRequest().body("El archivo está vacío.");
            }
            if (foto.getSize() > MAX_FOTO_BYTES) {
                return ResponseEntity.badRequest().body("El archivo supera el límite de 2 MB.");
            }
            String contentType = foto.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Solo se permiten archivos de imagen.");
            }

            String usuarioApp = extraerUsuarioApp(request);

            // Verificar que el usuario existe
            usuarioRepository.findByUsuarioApp(usuarioApp)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Subir a bucket dedicado "fotos-perfil"
            String urlPublica = storageService.subirArchivo(foto, "fotos-perfil", usuarioApp);

            // UPDATE directo sobre la columna — evita que JPA haga UPDATE completo
            // con campos que el usuario_bd del postulante no tiene permiso de tocar
            procedureRepository.actualizarFotoPerfil(usuarioApp, urlPublica);

            return ResponseEntity.ok(Map.of("fotoPerfil", urlPublica));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error al subir foto de perfil", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno al subir la foto.");
        }
    }

    // ─── Helper ────────────────────────────────────────────────────────────
    private String extraerUsuarioApp(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Token no proporcionado");
        }
        return jwtService.extractUsername(header.substring(7));
    }
}