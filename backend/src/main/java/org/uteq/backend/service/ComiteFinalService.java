package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.repository.PostgresProcedureRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ComiteFinalService {

    private final JdbcTemplate jdbc;
    private final PostgresProcedureRepository procedureRepo;

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
}