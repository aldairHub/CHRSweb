package org.uteq.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
// ❌ ELIMINADO: import org.uteq.backend.repository.IUsuarioRolRepository;
import org.uteq.backend.repository.PostgresProcedureRepository;  // ✅ NUEVO
import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.UsuarioRepository;

import java.util.HashSet;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ❌ ELIMINADO: private IUsuarioRolRepository usuarioRolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ILoginAuditService loginAuditService;

    @Autowired
    private DbSwitchService dbSwitchService;

    // ✅ NUEVO: Repository para stored procedures
    @Autowired
    private PostgresProcedureRepository postgresProcedureRepository;

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       DbSwitchService dbSwitchService,
                       PostgresProcedureRepository postgresProcedureRepository) {

        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.dbSwitchService = dbSwitchService;
        this.postgresProcedureRepository = postgresProcedureRepository;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        String usuarioApp = request.getUsuarioApp();

        // 1) Buscar usuario
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp).orElse(null);

        if (usuario == null) {
            safeAuditFail(usuarioApp, null, AuditLoginMotivo.USER_NOT_FOUND, httpRequest);
            throw new RuntimeException("Usuario no encontrado");
        }

        // 2) Inactivo
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            safeAuditFail(usuarioApp, usuario.getUsuarioBd(), AuditLoginMotivo.USER_DISABLED, httpRequest);
            throw new RuntimeException("Usuario inactivo");
        }

        // 3) Password incorrecta
        if (!passwordEncoder.matches(request.getClaveApp(), usuario.getClaveApp())) {
            safeAuditFail(usuarioApp, usuario.getUsuarioBd(), AuditLoginMotivo.BAD_CREDENTIALS, httpRequest);
            throw new RuntimeException("Contraseña incorrecta");
        }

        // ✅ 4) CAMBIO REAL DE CONEXIÓN BD
        dbSwitchService.switchToUser(usuario.getUsuarioBd(), usuario.getClaveBd());

        // ✅ 5) OBTENER ROLES desde PostgreSQL usando stored procedure
        //    Este procedure consulta pg_auth_members y mapea a roles de aplicación
        List<String> roles = postgresProcedureRepository.obtenerRolesAppUsuario();

        // Validar que tenga al menos un rol
        if (roles == null || roles.isEmpty()) {
            // Revertir conexión si no tiene roles
            dbSwitchService.resetToDefault();
            throw new RuntimeException("El usuario no tiene roles asignados en PostgreSQL");
        }

        safeAuditSuccess(usuarioApp, usuario.getUsuarioBd(), httpRequest);

        // 6) Generar JWT con roles de aplicación
        String token = jwtService.generateToken(usuario.getUsuarioApp(), roles);

        return new LoginResponse(token, usuario.getUsuarioApp(), new HashSet<>(roles));
    }

    private void safeAuditSuccess(String usuarioApp, String usuarioBd, HttpServletRequest httpRequest) {
        try {
            loginAuditService.logSuccess(usuarioApp, usuarioBd, httpRequest);
        } catch (Exception ignored) {}
    }

    private void safeAuditFail(String usuarioApp, String usuarioBdOrNull, AuditLoginMotivo motivo, HttpServletRequest httpRequest) {
        try {
            loginAuditService.logFail(usuarioApp, usuarioBdOrNull, motivo, httpRequest);
        } catch (Exception ignored) {}
    }
}
