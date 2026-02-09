package org.uteq.backend.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.Repository.IUsuarioRolRepository;
import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Repository.UsuarioRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private IUsuarioRolRepository usuarioRolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ILoginAuditService loginAuditService; //   módulo de auditoría

    @Autowired
    private final DbSwitchService dbSwitchService;

    public AuthService(UsuarioRepository usuarioRepository,
                       IUsuarioRolRepository usuarioRolRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       DbSwitchService dbSwitchService) {

        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.dbSwitchService = dbSwitchService;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        String usuarioApp = request.getUsuarioApp();

        // 1) Buscar usuario
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp).orElse(null);

        if (usuario == null) {
            // FAIL: usuario no existe
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


        //  CAMBIO REAL DE CONEXIÓN BD (DEMO)
        dbSwitchService.switchToUser(usuario.getUsuarioBd(), usuario.getClaveBd());
        // SUCCESS
        List<String> roles = usuarioRolRepository.findRoleNamesByUserId(usuario.getIdUsuario());
        safeAuditSuccess(usuarioApp, usuario.getUsuarioBd(), httpRequest);

        String token = jwtService.generateToken(usuario.getUsuarioApp(), roles);

        return new LoginResponse(token, usuario.getUsuarioApp(), new HashSet<>(roles));



//        return new LoginResponse(token, usuario.getUsuarioApp(),
//                Collections.singleton(usuario.getRol().name()));
    }

    // Para que auditoría NUNCA rompa el login
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