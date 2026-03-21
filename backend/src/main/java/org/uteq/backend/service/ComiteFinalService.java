package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ComiteFinalService {

    private static final Logger log = LoggerFactory.getLogger(ComiteFinalService.class);

    private final JdbcTemplate jdbc;
    private final PostgresProcedureRepository procedureRepo;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    // ── Obtener candidatos con puntajes para el panel comité ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerCandidatosComite(Long idSolicitud) {
        return jdbc.queryForList("""
            SELECT
                pe.id_proceso,
                pe.id_solicitud,
                p.nombres_postulante        AS nombres,
                p.apellidos_postulante      AS apellidos,
                COALESCE(pe.puntaje_matriz, 0)      AS puntaje_matriz,
                COALESCE(pe.puntaje_entrevista, 0)  AS puntaje_entrevista,
                COALESCE(pe.puntaje_matriz, 0) + COALESCE(pe.puntaje_entrevista, 0) AS puntaje_total,
                pe.decision_comite,
                pe.bloqueado,
                pe.habilitado_entrevista
            FROM proceso_evaluacion pe
            JOIN postulante p ON pe.id_postulante = p.id_postulante
            WHERE pe.id_solicitud = ?
            ORDER BY puntaje_total DESC
            """, idSolicitud);
    }

    // ── Verificar si ya fue confirmado el comité ──────────────
    @Transactional(readOnly = true)
    public boolean estaConfirmado(Long idSolicitud) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM proceso_evaluacion WHERE id_solicitud = ? AND bloqueado = TRUE",
                Integer.class, idSolicitud);
        return count != null && count > 0;
    }

    // ── Confirmar decisión final ──────────────────────────────
    public void confirmarDecision(Long idSolicitud, Long idProcesoGanador, String actaComite) {
        procedureRepo.ejecutarProcedure(
                "CALL sp_confirmar_decision_final(?, ?, ?)",
                idSolicitud, idProcesoGanador, actaComite);
    }

    // ── Verificar si un usuario es quien realizó la solicitud ──
    @Transactional(readOnly = true)
    public boolean esSolicitante(Long idSolicitud, String usuarioApp) {
        try {
            Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM solicitud_docente sd
                JOIN autoridad_academica aa ON sd.id_autoridad = aa.id_autoridad
                JOIN usuario u              ON aa.id_usuario   = u.id_usuario
                WHERE sd.id_solicitud = ?
                  AND u.usuario_app   = ?
                """, Integer.class, idSolicitud, usuarioApp);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Listar decisiones para el revisor ─────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDecisionesRevisor(String estado) {
        return jdbc.queryForList(
                "SELECT * FROM fn_listar_decisiones_revisor(?)", estado);
    }

    // ── Marcar decisión como revisada ─────────────────────────
    public void marcarRevisada(Long idDecision) {
        procedureRepo.ejecutarProcedure(
                "CALL sp_marcar_decision_revisada(?)", idDecision);
    }
    // ════════════════════════════════════════════════════════════════════════
// MÉTODOS NUEVOS — agregar a ComiteFinalService.java
// ════════════════════════════════════════════════════════════════════════

    // ── Paso 2: Notificar en app a revisor y postulantes ─────────────────
    public void notificarDecision(Long idSolicitud) {
        Map<String, Object> solicitud = jdbc.queryForMap("""
            SELECT m.nombre_materia, c.nombre_carrera
            FROM solicitud_docente sd
            JOIN materia m ON sd.id_materia = m.id_materia
            JOIN carrera c ON sd.id_carrera = c.id_carrera
            WHERE sd.id_solicitud = ?
            """, idSolicitud);

        String materia = (String) solicitud.get("nombre_materia");
        String carrera = (String) solicitud.get("nombre_carrera");

        Map<String, Object> ganador = jdbc.queryForMap("""
            SELECT p.nombres_postulante || ' ' || p.apellidos_postulante AS nombre
            FROM proceso_evaluacion pe
            JOIN postulante p ON pe.id_postulante = p.id_postulante
            WHERE pe.id_solicitud = ? AND pe.decision_comite = 'ganador'
            LIMIT 1
            """, idSolicitud);

        String nombreGanador = (String) ganador.get("nombre");

        notificacionService.notificarRol(
                "revisor", "success",
                "Decisión de comité lista para revisión",
                "El comité ha tomado una decisión final para " + materia + " (" + carrera + "). " +
                        "Candidato seleccionado: " + nombreGanador + ". Requiere tu revisión.",
                "SOLICITUD", idSolicitud
        );

        List<Map<String, Object>> postulantes = jdbc.queryForList("""
            SELECT
                p.id_usuario,
                p.nombres_postulante || ' ' || p.apellidos_postulante AS nombre,
                pe.decision_comite,
                pe.id_proceso
            FROM proceso_evaluacion pe
            JOIN postulante p ON pe.id_postulante = p.id_postulante
            WHERE pe.id_solicitud = ?
            """, idSolicitud);

        for (Map<String, Object> post : postulantes) {
            Long idUsuario = ((Number) post.get("id_usuario")).longValue();
            Long idProceso = ((Number) post.get("id_proceso")).longValue();
            boolean esGanador = "ganador".equals(post.get("decision_comite"));

            notificacionService.notificarUsuario(
                    idUsuario,
                    esGanador ? "success" : "info",
                    esGanador ? "¡Felicitaciones! Has sido seleccionado" : "Proceso de selección finalizado",
                    esGanador
                            ? "Has sido seleccionado como docente para " + materia + " en " + carrera + "."
                            : "El proceso de selección para " + materia + " ha finalizado. Gracias por participar.",
                    "PROCESO", idProceso
            );
        }
    }

    // ── Paso 3: Enviar correos (asíncrono) ───────────────────────────────
    @Async
    public void enviarCorreosDecision(Long idSolicitud) {
        List<Map<String, Object>> postulantes = jdbc.queryForList("""
            SELECT
                p.nombres_postulante || ' ' || p.apellidos_postulante AS nombre,
                p.correo_postulante                                    AS correo,
                pe.decision_comite,
                m.nombre_materia                                       AS materia,
                c.nombre_carrera                                       AS carrera,
                COALESCE(pe.puntaje_matriz, 0) +
                  COALESCE(pe.puntaje_entrevista, 0)                   AS puntaje_total
            FROM proceso_evaluacion pe
            JOIN postulante p        ON pe.id_postulante = p.id_postulante
            JOIN solicitud_docente sd ON pe.id_solicitud = sd.id_solicitud
            JOIN materia m           ON sd.id_materia    = m.id_materia
            JOIN carrera c           ON sd.id_carrera    = c.id_carrera
            WHERE pe.id_solicitud = ?
            """, idSolicitud);

        for (Map<String, Object> post : postulantes) {
            try {
                boolean esGanador = "ganador".equals(post.get("decision_comite"));
                emailService.enviarResultadoSeleccion(
                        (String) post.get("correo"),
                        (String) post.get("nombre"),
                        (String) post.get("materia"),
                        (String) post.get("carrera"),
                        esGanador,
                        post.get("puntaje_total") != null ? post.get("puntaje_total").toString() : "—"
                );
            } catch (Exception e) {
                log.error("Error enviando correo a {}: {}", post.get("correo"), e.getMessage());
            }
        }
    }

    // ── Detalle completo para el revisor ─────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleDecision(Long idSolicitud) {
        Map<String, Object> info = jdbc.queryForMap("""
            SELECT
                dr.id_decision, dr.nombre_ganador, dr.puntaje_final,
                dr.acta_comite, dr.estado, dr.fecha_envio,
                m.nombre_materia  AS materia,
                c.nombre_carrera  AS carrera,
                f.nombre_facultad AS facultad
            FROM decision_revisor dr
            JOIN solicitud_docente sd ON dr.id_solicitud = sd.id_solicitud
            JOIN materia m  ON sd.id_materia = m.id_materia
            JOIN carrera c  ON sd.id_carrera = c.id_carrera
            JOIN facultad f ON c.id_facultad = f.id_facultad
            WHERE dr.id_solicitud = ?
            ORDER BY dr.fecha_envio DESC LIMIT 1
            """, idSolicitud);

        List<Map<String, Object>> candidatos = jdbc.queryForList("""
            SELECT
                p.nombres_postulante || ' ' || p.apellidos_postulante AS nombre,
                pe.decision_comite,
                COALESCE(pe.puntaje_matriz, 0)                        AS puntaje_meritos,
                COALESCE(pe.puntaje_entrevista, 0)                    AS puntaje_entrevista,
                COALESCE(pe.puntaje_matriz, 0) +
                  COALESCE(pe.puntaje_entrevista, 0)                  AS puntaje_total,
                pe.justificacion_habilitacion                         AS justificacion_decision,
                pe.habilitado_entrevista,
                pe.id_proceso
            FROM proceso_evaluacion pe
            JOIN postulante p ON pe.id_postulante = p.id_postulante
            WHERE pe.id_solicitud = ?
            ORDER BY puntaje_total DESC
            """, idSolicitud);

        for (Map<String, Object> cand : candidatos) {
            Long idProceso = ((Number) cand.get("id_proceso")).longValue();

            // Fases de entrevista — fase_proceso no tiene observaciones ni puntaje_obtenido,
            // usa calificacion y estado
            List<Map<String, Object>> fases = jdbc.queryForList("""
                SELECT
                    fe.nombre  AS nombre_fase,
                    COALESCE(fp.calificacion, 0) AS puntaje_obtenido,
                    fp.estado
                FROM fase_proceso fp
                JOIN fase_evaluacion fe ON fp.id_fase = fe.id_fase
                WHERE fp.id_proceso = ?
                ORDER BY fe.orden
                """, idProceso);
            cand.put("fases_entrevista", fases);

            // Puntajes por sección de méritos
            List<Map<String, Object>> seccionesPuntaje = jdbc.queryForList("""
                SELECT
                    ms.titulo         AS seccion,
                    ms.puntaje_maximo,
                    COALESCE(SUM(mmp.valor::numeric), 0) AS subtotal
                FROM matriz_seccion ms
                LEFT JOIN matriz_item mi ON mi.id_seccion = ms.id_seccion AND mi.activo = TRUE
                LEFT JOIN matriz_meritos_puntaje mmp
                       ON mmp.item_id = mi.codigo AND mmp.id_proceso = ?
                WHERE ms.activo = TRUE
                GROUP BY ms.id_seccion, ms.titulo, ms.puntaje_maximo, ms.orden
                ORDER BY ms.orden
                """, idProceso);
            cand.put("detalle_secciones", seccionesPuntaje);
        }

        info.put("candidatos", candidatos);
        return info;
    }

    // ── Enviar informe PDF a la autoridad académica ──────────────────────
    @Async
    public void enviarInformeAAutoridad(Long idSolicitud, byte[] pdfBytes) {
        try {
            Map<String, Object> datos = jdbc.queryForMap("""
                SELECT
                    aa.correo                                          AS correo_autoridad,
                    aa.nombres || ' ' || aa.apellidos                 AS nombre_autoridad,
                    m.nombre_materia                                   AS materia,
                    c.nombre_carrera                                   AS carrera,
                    p.nombres_postulante || ' ' || p.apellidos_postulante AS nombre_ganador
                FROM solicitud_docente sd
                JOIN autoridad_academica aa ON sd.id_autoridad  = aa.id_autoridad
                JOIN materia m              ON sd.id_materia    = m.id_materia
                JOIN carrera c              ON sd.id_carrera    = c.id_carrera
                JOIN proceso_evaluacion pe  ON pe.id_solicitud  = sd.id_solicitud
                                           AND pe.decision_comite = 'ganador'
                JOIN postulante p           ON pe.id_postulante = p.id_postulante
                WHERE sd.id_solicitud = ?
                LIMIT 1
                """, idSolicitud);

            emailService.enviarInformeFinalConPdf(
                    (String) datos.get("correo_autoridad"),
                    (String) datos.get("nombre_autoridad"),
                    (String) datos.get("materia"),
                    (String) datos.get("carrera"),
                    (String) datos.get("nombre_ganador"),
                    pdfBytes
            );
        } catch (Exception e) {
            log.error("Error enviando informe a autoridad para solicitud {}: {}", idSolicitud, e.getMessage());
        }
    }
}