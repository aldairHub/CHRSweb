package org.uteq.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.*;
import org.uteq.backend.entity.RolApp;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j

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
        log.info("[LOGIN] Intento de login para: {}", usuarioApp);

        // 1) Buscar usuario
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp).orElse(null);
        if (usuario == null) {
            log.warn("[LOGIN] Usuario no encontrado: {}", usuarioApp);
            safeAuditFail(usuarioApp, null, null, AuditLoginMotivo.USER_NOT_FOUND, httpRequest);
            throw new RuntimeException("Usuario no encontrado");
        }
        log.info("[LOGIN] Usuario encontrado: id={}, usuarioBd={}", usuario.getIdUsuario(), usuario.getUsuarioBd());

        // 2) Inactivo
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn("[LOGIN] Usuario inactivo: {}", usuarioApp);
            safeAuditFail(usuarioApp, usuario.getUsuarioBd(), usuario.getIdUsuario(),
                    AuditLoginMotivo.USER_DISABLED, httpRequest);
            throw new RuntimeException("Usuario inactivo");
        }

        // 3) Password
        if (!passwordEncoder.matches(request.getClaveApp(), usuario.getClaveApp())) {
            log.warn("[LOGIN] Contraseña incorrecta para: {}", usuarioApp);
            safeAuditFail(usuarioApp, usuario.getUsuarioBd(), usuario.getIdUsuario(),
                    AuditLoginMotivo.BAD_CREDENTIALS, httpRequest);
            throw new RuntimeException("Contraseña incorrecta");
        }
        log.info("[LOGIN] Contraseña correcta para: {}", usuarioApp);

        // 4) Switch de conexión
        log.info("[LOGIN] Haciendo switch al usuario BD: {}", usuario.getUsuarioBd());
        try {
            String claveBdReal = aesCipherService.descifrar(usuario.getClaveBd());
            dbSwitchService.switchToUser(usuario.getUsuarioBd(), claveBdReal);
            log.info("[LOGIN] Switch exitoso");
        } catch (Exception e) {
            log.error("[LOGIN] Error en switch de conexión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al conectar con la base de datos: " + e.getMessage());
        }

        // 5) Obtener RolApps del usuario via SP (id + nombre, requiere conexión del usuario activa)
        log.info("[LOGIN] Cargando RolApps del usuario via SP...");
        List<Map<String, Object>> rolesRaw;
        try {
            rolesRaw = postgresProcedureRepository.obtenerRolesAppConIdUsuario(usuarioApp);
            log.info("[LOGIN] RolApps encontrados: {}", rolesRaw.size());
            rolesRaw.forEach(r -> log.info("[LOGIN]   -> RolApp: id={}, nombre={}", r.get("id_rol_app"), r.get("nombre")));
        } catch (Exception e) {
            log.error("[LOGIN] Error al cargar RolApps via SP: {}", e.getMessage(), e);
            dbSwitchService.resetToDefault();
            throw new RuntimeException("Error al cargar roles del usuario: " + e.getMessage());
        }

        if (rolesRaw.isEmpty()) {
            log.warn("[LOGIN] El usuario no tiene RolApps asignados");
            dbSwitchService.resetToDefault();
            throw new RuntimeException("El usuario no tiene roles de aplicación asignados");
        }

        // Primer rol como activo por defecto
        Integer idRolActivo     = (Integer) rolesRaw.get(0).get("id_rol_app");
        String  nombreRolActivo = (String)  rolesRaw.get(0).get("nombre");
        log.info("[LOGIN] Rol activo por defecto: id={}, nombre={}", idRolActivo, nombreRolActivo);

        // 6) Preparar lista de roles disponibles para el frontend (selector de rol)
        List<RolAppDTO> rolesDisponibles = rolesRaw.stream()
                .map(r -> {
                    RolAppDTO dto = new RolAppDTO();
                    dto.setIdRolApp((Integer) r.get("id_rol_app"));
                    dto.setNombre((String)    r.get("nombre"));
                    return dto;
                })
                .collect(Collectors.toList());

        // 7) Obtener nombres de roles de BD via SP (para el JWT)
        log.info("[LOGIN] Llamando a sp_obtener_roles_app_usuario()...");
        List<String> roles;
        try {
            roles = postgresProcedureRepository.obtenerRolesAppUsuario();
            log.info("[LOGIN] Roles obtenidos del SP: {}", roles);
        } catch (Exception e) {
            log.error("[LOGIN] Error en sp_obtener_roles_app_usuario: {}", e.getMessage(), e);
            dbSwitchService.resetToDefault();
            throw new RuntimeException("Error al obtener roles: " + e.getMessage());
        }

        if (roles == null || roles.isEmpty()) {
            log.warn("[LOGIN] El SP no devolvió roles para: {}", usuarioApp);
            dbSwitchService.resetToDefault();
            throw new RuntimeException("El usuario no tiene roles asignados en PostgreSQL");
        }

        // 8) Obtener menú del rol activo via SP
        log.info("[LOGIN] Llamando a sp_obtener_opciones_usuario({})...", idRolActivo);
        ModuloOpcionesDTO modulo;
        try {
            modulo = postgresProcedureRepository.obtenerOpcionesUsuario(idRolActivo);
            if (modulo != null) {
                log.info("[LOGIN] Módulo obtenido: nombre={}, opciones={}",
                        modulo.getModuloNombre(),
                        modulo.getOpciones() != null ? modulo.getOpciones().size() : 0);
            } else {
                log.warn("[LOGIN] sp_obtener_opciones_usuario devolvió null para idRolApp={}", idRolActivo);
            }
        } catch (Exception e) {
            log.error("[LOGIN] Error en sp_obtener_opciones_usuario: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener menú: " + e.getMessage());
        }

        // 9) Generar JWT
        log.info("[LOGIN] Generando JWT...");
        String token = jwtService.generateToken(
                usuario.getUsuarioApp(),
                roles,
                usuario.getTokenVersion()
        );

        // 10) Auditar después de que todo fue exitoso
        safeAuditSuccess(usuarioApp, usuario.getUsuarioBd(), usuario.getIdUsuario(), httpRequest);

        log.info("[LOGIN] Login completado exitosamente para: {}", usuarioApp);

        // 11) Retornar con flag de primer login
        return new LoginResponse(
                token,
                usuario.getUsuarioApp(),
                new HashSet<>(roles),
                usuario.getPrimerLogin(),
                usuario.getIdUsuario(),
                modulo,
                nombreRolActivo,
                rolesDisponibles
        );
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