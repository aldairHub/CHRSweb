package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.DbSwitchService;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import org.uteq.backend.service.AuthService;
import org.uteq.backend.service.JwtService;
import org.uteq.backend.service.LoginAuditService;

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

    public AuthController(AuthService authService,
                          DbSwitchService dbSwitchService,
                          JwtService jwtService,
                          UsuarioRepository usuarioRepository,
                          LoginAuditService loginAuditService) {
        this.authService = authService;
        this.dbSwitchService = dbSwitchService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.loginAuditService = loginAuditService;
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
                    usuarioRepository.findByUsuarioApp(usuarioApp).ifPresent(u ->
                            loginAuditService.logLogout(
                                    u.getUsuarioApp(),
                                    u.getUsuarioBd(),
                                    u.getIdUsuario(),
                                    request
                            )
                    );
                }
            } catch (Exception ignored) {
                // Token expirado o inválido — igual hacemos logout
            }
        }

        // 3) Resetear conexión BD
        dbSwitchService.resetToDefault();
        System.out.println("Deslogueando");
        return ResponseEntity.ok("Logout OK");
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