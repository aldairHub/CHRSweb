package org.uteq.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.uteq.backend.entity.AudAccion;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.AudAccionRepository;
import org.uteq.backend.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio de auditoría extendida.
 *
 * Registra de forma asíncrona las acciones importantes del sistema.
 * Vincula cada registro con el Usuario de base de datos mediante
 * id_usuario (FK) y almacena también usuario_app y usuario_bd
 * para trazabilidad completa.
 *
 * Cuando el cambio viene directo por SQL (trigger):
 *   - id_usuario  = NULL
 *   - usuario_app = 'db_directo'
 *   - usuario_bd  = current_user de PostgreSQL
 *
 * Cuando el cambio viene desde la app Java:
 *   - id_usuario  = FK al Usuario autenticado
 *   - usuario_app = correo del JWT
 *   - usuario_bd  = usuario_bd del Usuario autenticado
 */
@Service
@RequiredArgsConstructor
public class AccionAuditoriaService {

    private final AudAccionRepository audRepo;
    private final UsuarioRepository   usuarioRepo;

    // ─── Método principal — recibe el usuarioApp del JWT ─────────────────

    @Async
    public void registrar(
            String              usuarioApp,
            String              accion,
            String              entidad,
            Long                idEntidad,
            String              descripcion,
            String              ipCliente
    ) {
        AudAccion aud = new AudAccion();

        // Buscar el Usuario en BD para obtener id_usuario y usuario_bd
        if (usuarioApp != null && !usuarioApp.isBlank()) {
            usuarioRepo.findByUsuarioApp(usuarioApp).ifPresentOrElse(
                    u -> {
                        aud.setUsuario(u);
                        aud.setUsuarioApp(u.getUsuarioApp());
                        aud.setUsuarioBd(u.getUsuarioBd());
                    },
                    () -> {
                        // usuario_app llegó en el JWT pero no existe en BD (caso raro)
                        aud.setUsuario(null);
                        aud.setUsuarioApp(usuarioApp);
                        aud.setUsuarioBd("desconocido");
                    }
            );
        } else {
            aud.setUsuario(null);
            aud.setUsuarioApp("sistema");
            aud.setUsuarioBd("sistema");
        }

        aud.setAccion(accion);
        aud.setEntidad(entidad);
        aud.setIdEntidad(idEntidad);
        aud.setDescripcion(descripcion);
        aud.setIpCliente(ipCliente);
        aud.setFecha(LocalDateTime.now());

        audRepo.save(aud);
    }

    // ─── Sobrecarga sin idEntidad ─────────────────────────────────────────

    @Async
    public void registrar(
            String usuarioApp,
            String accion,
            String entidad,
            String descripcion,
            String ipCliente
    ) {
        registrar(usuarioApp, accion, entidad, null, descripcion, ipCliente);
    }

    // ─── Extrae IP real del request HTTP ─────────────────────────────────

    public String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ─── Consulta paginada para el panel de auditoría ────────────────────

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Page<AudAccion> buscar(
            String    usuarioApp,
            String    usuarioBd,
            String    accion,
            String    entidad,
            LocalDate desde,
            LocalDate hasta,
            Pageable  pageable
    ) {
        String desdeStr = desde != null ? desde.atStartOfDay().format(TS_FMT)             : null;
        String hastaStr = hasta != null ? hasta.plusDays(1).atStartOfDay().format(TS_FMT) : null;

        return audRepo.buscar(
                blankToNull(usuarioApp),
                blankToNull(usuarioBd),
                blankToNull(accion),
                blankToNull(entidad),
                desdeStr,
                hastaStr,
                pageable
        );
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}