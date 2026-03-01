package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ModuloOpcionesDTO;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.*;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import java.util.Map;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final DbSwitchService dbSwitchService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final LoginAuditService loginAuditService;
    private final PostgresProcedureRepository postgresProcedureRepository;
    private final AesCipherService aesCipherService;


    public AuthController(AuthService authService,
                          DbSwitchService dbSwitchService,
                          JwtService jwtService,
                          UsuarioRepository usuarioRepository,
                          LoginAuditService loginAuditService,
                          PostgresProcedureRepository postgresProcedureRepository,
                          AesCipherService aesCipherService) {
        this.authService = authService;
        this.dbSwitchService = dbSwitchService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.loginAuditService = loginAuditService;
        this.postgresProcedureRepository = postgresProcedureRepository;
        this.aesCipherService = aesCipherService;

    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.login(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Credenciales inválidas");

        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // 1) Extraer token del header
        String token = extraerToken(request);

        // 2) Si hay token válido, auditar el logout
        if (token != null) {
            try {
                String usuarioApp = jwtService.extractUsername(token);
                if (usuarioApp != null) {
                    // Invalidar via SP — incrementa token_version en BD
                    postgresProcedureRepository.invalidarTokenUsuario(usuarioApp);
                    usuarioRepository.findByUsuarioApp(usuarioApp).ifPresent(u ->
                            loginAuditService.logLogout(
                                    u.getUsuarioApp(), u.getUsuarioBd(),
                                    u.getIdUsuario(), request
                            )
                    );
                }
            } catch (Exception ignored) {}
            // Token expirado o inválido — igual hacemos logout
        }
        dbSwitchService.resetToDefault();
        System.out.println("Deslogueando");
        return ResponseEntity.ok("Logout OK");

    }
    @GetMapping("/menu/{idRolApp}")
    public ResponseEntity<?> menuPorRol(@PathVariable Integer idRolApp,
                                        HttpServletRequest request) {
        String token = extraerToken(request);
        if (token == null || !jwtService.isTokenValid(token))
            return ResponseEntity.status(401).build();

        String usuarioApp = jwtService.extractUsername(token);

        // 1) Buscar usuario — solo campos simples, sin tocar rolesApp (lazy)
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp).orElse(null);
        if (usuario == null) return ResponseEntity.status(403).build();

        // 2) Switch al usuario de BD
        try {
            String claveBdReal = aesCipherService.descifrar(usuario.getClaveBd());
            dbSwitchService.switchToUser(usuario.getUsuarioBd(), claveBdReal);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error de conexión");
        }

        // 3) Verificar que el usuario tiene ese rol via SP (sin JPA)
        List<Map<String, Object>> rolesRaw = postgresProcedureRepository.obtenerRolesAppConIdUsuario(usuarioApp);
        boolean tieneRol = rolesRaw.stream()
                .anyMatch(r -> idRolApp.equals(r.get("id_rol_app")));
        if (!tieneRol) return ResponseEntity.status(403).build();

        // 4) Obtener menú
        ModuloOpcionesDTO modulo = postgresProcedureRepository.obtenerOpcionesUsuario(idRolApp);
        return ResponseEntity.ok(modulo);
    }
    // ─── Helper ────────────────────────────────────────────────

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}