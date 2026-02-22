package org.uteq.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.UsuarioRepository;

import java.util.HashSet;
import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAuditService loginAuditService;
    private final DbSwitchService dbSwitchService;
    private final PostgresProcedureRepository postgresProcedureRepository;
    private final AesCipherService aesCipherService;

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       DbSwitchService dbSwitchService,
                       PostgresProcedureRepository postgresProcedureRepository,
                       AesCipherService aesCipherService,
                       LoginAuditService loginAuditService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.dbSwitchService = dbSwitchService;
        this.postgresProcedureRepository = postgresProcedureRepository;
        this.aesCipherService = aesCipherService;
        this.loginAuditService = loginAuditService;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        String usuarioApp = request.getUsuarioApp();

        // 1) Buscar usuario
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp).orElse(null);
        if (usuario == null) {
            safeAuditFail(usuarioApp, null, null,
                    AuditLoginMotivo.USER_NOT_FOUND, httpRequest);
            throw new RuntimeException("Usuario no encontrado");
        }

        // 2) Inactivo
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            safeAuditFail(usuarioApp, usuario.getUsuarioBd(), usuario.getIdUsuario(),
                    AuditLoginMotivo.USER_DISABLED, httpRequest);
            throw new RuntimeException("Usuario inactivo");
        }

        // 3) Password incorrecta
        if (!passwordEncoder.matches(request.getClaveApp(), usuario.getClaveApp())) {
            safeAuditFail(usuarioApp, usuario.getUsuarioBd(), usuario.getIdUsuario(),
                    AuditLoginMotivo.BAD_CREDENTIALS, httpRequest);
            throw new RuntimeException("Contraseña incorrecta");
        }

        // 4) Switch de conexión
        String claveBdReal = aesCipherService.descifrar(usuario.getClaveBd());
        dbSwitchService.switchToUser(usuario.getUsuarioBd(), claveBdReal);

        // 5) Obtener roles
        List<String> roles = postgresProcedureRepository.obtenerRolesAppUsuario();
        if (roles == null || roles.isEmpty()) {
            dbSwitchService.resetToDefault();
            throw new RuntimeException("El usuario no tiene roles asignados en PostgreSQL");
        }

        // 6) Generar JWT
        String token = jwtService.generateToken(usuario.getUsuarioApp(), roles);

        // 7) Auditar DESPUÉS de que todo fue exitoso
        safeAuditSuccess(usuarioApp, usuario.getUsuarioBd(),
                usuario.getIdUsuario(), httpRequest);

        return new LoginResponse(token, usuario.getUsuarioApp(), new HashSet<>(roles));
    }

    // ─── Helpers auditoría ─────────────────────────────────────

    private void safeAuditSuccess(String usuarioApp, String usuarioBd,
                                  Long idUsuario, HttpServletRequest request) {
        try {
            loginAuditService.logSuccess(usuarioApp, usuarioBd, idUsuario, request);
        } catch (Exception ignored) {}
    }

    private void safeAuditFail(String usuarioApp, String usuarioBd,
                               Long idUsuario, AuditLoginMotivo motivo,
                               HttpServletRequest request) {
        try {
            loginAuditService.logFail(usuarioApp, usuarioBd, idUsuario, motivo, request);
        } catch (Exception ignored) {}
    }
}