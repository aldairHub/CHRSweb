package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.NotificacionDTO;
import org.uteq.backend.dto.NotificacionesResumenDTO;
import org.uteq.backend.repository.NotificacionRepository;
import org.uteq.backend.repository.UsuarioRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notifRepo;
    private final UsuarioRepository      usuarioRepo;
    private final JdbcTemplate           jdbc;

    // =========================================================================
    // MÉTODOS PÚBLICOS — CREAR NOTIFICACIONES
    // Llamados desde otros servicios cuando ocurre un evento
    // =========================================================================

    /**
     * Notifica a un usuario concreto (por su id_usuario).
     */
    @Async
    public void notificarUsuario(Long idUsuario, String tipo, String titulo,
                                  String mensaje, String entidadTipo, Long entidadId) {
        try {
            jdbc.queryForObject(
                "SELECT sp_crear_notificacion(?, ?, ?, ?, ?, ?)",
                Long.class,
                idUsuario, tipo, titulo, mensaje, entidadTipo, entidadId
            );
        } catch (Exception e) {
            log.error("Error al crear notificacion para usuario {}: {}", idUsuario, e.getMessage());
        }
    }

    /**
     * Notifica a todos los usuarios que tengan un nombre de rol que contenga p_nombreRol.
     * Ej: notificarRol("revisor", ...) → avisa a todos los revisores.
     * Ej: notificarRol("admin",   ...) → avisa a todos los admins.
     */
    @Async
    public void notificarRol(String nombreRol, String tipo, String titulo,
                              String mensaje, String entidadTipo, Long entidadId) {
        try {
            List<Map<String, Object>> usuarios = jdbc.queryForList(
                "SELECT id_usuario FROM sp_notif_usuarios_por_rol(?)", nombreRol
            );

            for (Map<String, Object> row : usuarios) {
                Long idUsuario = ((Number) row.get("id_usuario")).longValue();
                notificarUsuario(idUsuario, tipo, titulo, mensaje, entidadTipo, entidadId);
            }
            log.info("Notificacion '{}' enviada a {} usuarios con rol '{}'",
                     titulo, usuarios.size(), nombreRol);
        } catch (Exception e) {
            log.error("Error al notificar rol '{}': {}", nombreRol, e.getMessage());
        }
    }

    // =========================================================================
    // EVENTOS DEL SISTEMA — llamar desde los servicios correspondientes
    // =========================================================================

    // ── POSTULANTE ──────────────────────────────────────────────────────────

    /** Llama desde PrepostulacionService.actualizarEstado() cuando estado = APROBADO */
    public void notifPostulanteAprobado(Long idUsuarioPostulante, Long idPrepostulacion) {
        notificarUsuario(
            idUsuarioPostulante, "success",
            "¡Prepostulación aprobada!",
            "Tu prepostulación fue aprobada. Revisa tu correo para obtener tus credenciales de acceso.",
            "PREPOSTULACION", idPrepostulacion
        );
    }

    /** Llama desde PrepostulacionService.actualizarEstado() cuando estado = RECHAZADO */
    public void notifPostulanteRechazado(Long idUsuarioPostulante, Long idPrepostulacion, String motivo) {
        notificarUsuario(
            idUsuarioPostulante, "error",
            "Prepostulación rechazada",
            "Tu prepostulación no fue aprobada. " +
            (motivo != null && !motivo.isBlank() ? "Motivo: " + motivo : "Comunícate con la institución para más información."),
            "PREPOSTULACION", idPrepostulacion
        );
    }

    /** Llama desde ReunionService.programar() o sp_agendar_entrevista */
    public void notifPostulanteEntrevistaProgramada(Long idUsuarioPostulante, Long idReunion,
                                                    String fecha, String hora, String modalidad) {
        String detalle = "Fecha: " + fecha + " a las " + hora + " — Modalidad: " + modalidad;
        notificarUsuario(
            idUsuarioPostulante, "info",
            "Entrevista programada",
            "Se ha programado tu entrevista. " + detalle,
            "REUNION", idReunion
        );
    }

    /** Llama desde ProcesoEvaluacionService cuando se cierra el proceso */
    public void notifPostulanteProcesoCerrado(Long idUsuarioPostulante, Long idProceso, String decision) {
        boolean aprobado = decision != null && decision.startsWith("aprobado");
        notificarUsuario(
            idUsuarioPostulante,
            aprobado ? "success" : "info",
            aprobado ? "¡Proceso finalizado — Seleccionado!" : "Proceso de evaluación cerrado",
            aprobado
                ? "Felicitaciones, has sido seleccionado en el proceso de evaluación docente."
                : "El proceso de evaluación docente en el que participaste ha finalizado.",
            "PROCESO", idProceso
        );
    }

    /** Llama cuando al postulante le faltan documentos por completar */
    public void notifPostulanteFaltanDocumentos(Long idUsuarioPostulante, Long idProceso, String detalle) {
        notificarUsuario(
            idUsuarioPostulante, "warning",
            "Documentos pendientes",
            "Tienes documentos pendientes por subir. " +
            (detalle != null ? detalle : "Ingresa al sistema para completarlos."),
            "PROCESO", idProceso
        );
    }

    // ── REVISOR ─────────────────────────────────────────────────────────────

    /** Llama desde PrepostulacionService.procesarPrepostulacion() — nueva prepostulación recibida */
    public void notifRevisorNuevaPrepostulacion(Long idPrepostulacion, String nombrePostulante) {
        notificarRol(
            "revisor", "info",
            "Nueva prepostulación recibida",
            "El postulante " + nombrePostulante + " ha enviado una prepostulación pendiente de revisión.",
            "PREPOSTULACION", idPrepostulacion
        );
    }

    /** Llama desde SolicitudDocenteService.crearSolicitud() */
    public void notifRevisorNuevaSolicitudDocente(Long idSolicitud, String materia, String carrera) {
        notificarRol(
            "revisor", "warning",
            "Nueva solicitud de docente",
            "Solicitud de docente para " + materia + " (" + carrera + ") requiere tu revisión.",
            "SOLICITUD", idSolicitud
        );
    }

    // ── EVALUADOR ───────────────────────────────────────────────────────────

    /** Llama desde ProcesoEvaluacionService cuando se asigna evaluador a un proceso */
    public void notifEvaluadorPostulanteAsignado(Long idUsuarioEvaluador, Long idProceso,
                                                  String nombrePostulante) {
        notificarUsuario(
            idUsuarioEvaluador, "info",
            "Nuevo postulante asignado",
            "Se te ha asignado evaluar al postulante " + nombrePostulante + ".",
            "PROCESO", idProceso
        );
    }

    /** Llama desde ReunionService.programar() para cada evaluador de la reunión */
    public void notifEvaluadorReunionProgramada(Long idUsuarioEvaluador, Long idReunion,
                                                 String nombrePostulante, String fecha, String hora) {
        notificarUsuario(
            idUsuarioEvaluador, "warning",
            "Reunión de entrevista programada",
            "Tienes una reunión con " + nombrePostulante + " el " + fecha + " a las " + hora + ".",
            "REUNION", idReunion
        );
    }

    /** Llama cuando queda evaluación pendiente de confirmar */
    public void notifEvaluadorEvaluacionPendiente(Long idUsuarioEvaluador, Long idReunion,
                                                   String nombrePostulante) {
        notificarUsuario(
            idUsuarioEvaluador, "warning",
            "Evaluación pendiente",
            "Tienes pendiente confirmar tu evaluación del postulante " + nombrePostulante + ".",
            "REUNION", idReunion
        );
    }

    // ── ADMIN ────────────────────────────────────────────────────────────────

    /** Llama desde UsuarioAdminService / AutoridadAcademicaService cuando se crea un usuario */
    public void notifAdminUsuarioCreado(Long idUsuarioNuevo, String nombreCompleto, String rol) {
        notificarRol(
            "admin", "success",
            "Nuevo usuario registrado",
            "Se registró el usuario " + nombreCompleto + " con rol: " + rol + ".",
            "USUARIO", idUsuarioNuevo
        );
    }

    /** Llama cuando hay un error crítico o acción que requiere atención del admin */
    public void notifAdminAlerta(String titulo, String detalle) {
        notificarRol("admin", "error", titulo, detalle, null, null);
    }

    // =========================================================================
    // CONSULTA — endpoints del controller
    // =========================================================================

    /**
     * Devuelve las últimas notificaciones + conteo no leídas usando el SP.
     */
    @Transactional(readOnly = true)
    public NotificacionesResumenDTO obtenerResumen(Long idUsuario) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM sp_notificaciones_usuario(?, FALSE, 30)", idUsuario
        );

        List<NotificacionDTO> lista = rows.stream()
            .map(row -> NotificacionDTO.builder()
                .idNotificacion(toLong(row.get("id_notificacion")))
                .tipo((String) row.get("tipo"))
                .titulo((String) row.get("titulo"))
                .mensaje((String) row.get("mensaje"))
                .leida((Boolean) row.get("leida"))
                .entidadTipo((String) row.get("entidad_tipo"))
                .entidadId(toLong(row.get("entidad_id")))
                .fechaCreacion(toLocalDateTime(row.get("fecha_creacion")))
                .tiempoRelativo((String) row.get("tiempo_relativo"))
                .build())
            .collect(Collectors.toList());

        int noLeidas = jdbc.queryForObject(
            "SELECT sp_contar_no_leidas(?)", Integer.class, idUsuario
        );

        return new NotificacionesResumenDTO(noLeidas, lista);
    }

    /**
     * Marca una notificación como leída usando el SP.
     */
    @Transactional
    public boolean marcarLeida(Long idNotificacion, Long idUsuario) {
        try {
            jdbc.update("CALL sp_marcar_notificacion_leida(?, ?, NULL, NULL)",
                        idNotificacion, idUsuario);
            return true;
        } catch (Exception e) {
            log.error("Error al marcar notificacion {} como leida: {}", idNotificacion, e.getMessage());
            return false;
        }
    }

    /**
     * Marca todas las notificaciones del usuario como leídas usando el SP.
     */
    @Transactional
    public int marcarTodasLeidas(Long idUsuario) {
        try {
            return notifRepo.marcarTodasLeidas(idUsuario);
        } catch (Exception e) {
            log.error("Error al marcar todas como leidas para usuario {}: {}", idUsuario, e.getMessage());
            return 0;
        }
    }

    // =========================================================================
    // LIMPIEZA AUTOMÁTICA — ejecuta cada domingo a medianoche
    // =========================================================================

    @Scheduled(cron = "0 0 0 * * SUN")
    public void limpiarNotificacionesAntiguas() {
        try {
            jdbc.update("CALL sp_eliminar_notificaciones_antiguas(90, NULL, NULL)");
            log.info("Limpieza de notificaciones antiguas completada");
        } catch (Exception e) {
            log.error("Error en limpieza de notificaciones: {}", e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Long toLong(Object val) {
        if (val == null) return null;
        return ((Number) val).longValue();
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof Timestamp) return ((Timestamp) val).toLocalDateTime();
        return null;
    }
}
