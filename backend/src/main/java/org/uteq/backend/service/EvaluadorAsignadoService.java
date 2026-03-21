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
public class EvaluadorAsignadoService {

    private final JdbcTemplate jdbc;
    private final PostgresProcedureRepository procedureRepo;

    // ── Verificar si un usuario puede evaluar ─────────────────
    @Transactional(readOnly = true)
    public boolean puedeEvaluar(Long idProceso, Long idUsuario, String tipo) {
        Boolean resultado = jdbc.queryForObject(
                "SELECT fn_puede_evaluar_proceso(?, ?, ?)",
                Boolean.class, idProceso, idUsuario, tipo);
        return Boolean.TRUE.equals(resultado);
    }

    // ── Listar evaluadores asignados a un proceso ─────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarEvaluadoresProceso(Long idProceso, String tipo) {
        return jdbc.queryForList(
                "SELECT * FROM fn_listar_evaluadores_proceso(?, ?)", idProceso, tipo);
    }

    // ── Listar evaluadores disponibles para asignar ───────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDisponibles(Long idProceso, String tipo) {
        return jdbc.queryForList(
                "SELECT * FROM fn_listar_evaluadores_disponibles(?, ?)", idProceso, tipo);
    }

    // ── Listar evaluadores disponibles filtrados por facultad de la solicitud ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDisponiblesPorFacultad(Long idProceso, String tipo, Long idSolicitud) {
        return jdbc.queryForList("""
            SELECT DISTINCT
                u.id_usuario        AS v_id_usuario,
                CONCAT(aa.nombres, ' ', aa.apellidos) AS v_nombre_completo,
                u.usuario_app       AS v_usuario_app,
                false               AS v_es_dueno
            FROM autoridad_academica aa
            JOIN usuario u            ON aa.id_usuario = u.id_usuario
            JOIN solicitud_docente sd ON sd.id_solicitud = ?
            JOIN carrera car          ON sd.id_carrera = car.id_carrera
            WHERE aa.id_facultad = car.id_facultad
              AND u.id_usuario NOT IN (
                  SELECT ea.id_usuario
                  FROM proceso_evaluador_asignado ea
                  WHERE ea.id_proceso = ?
                    AND ea.tipo = ?
              )
              AND aa.estado = true
            ORDER BY v_nombre_completo
            """, idSolicitud, idProceso, tipo);
    }

    // ── Asignar evaluador ─────────────────────────────────────
    public void asignar(Long idProceso, Long idUsuario, String tipo) {
        procedureRepo.ejecutarProcedure(
                "CALL sp_asignar_evaluador_proceso(?, ?, ?)", idProceso, idUsuario, tipo);
    }

    // ── Quitar evaluador ──────────────────────────────────────
    public void quitar(Long idProceso, Long idUsuario, String tipo) {
        procedureRepo.ejecutarProcedure(
                "CALL sp_quitar_evaluador_proceso(?, ?, ?)", idProceso, idUsuario, tipo);
    }

    // ── Obtener id_usuario desde usuario_app (para el JWT) ────
    @Transactional(readOnly = true)
    public Long obtenerIdUsuario(String usuarioApp) {
        try {
            return jdbc.queryForObject(
                    "SELECT id_usuario FROM usuario WHERE usuario_app = ?",
                    Long.class, usuarioApp);
        } catch (Exception e) {
            return null;
        }
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerProcesosPorSolicitud(Long idSolicitud) {
        return jdbc.queryForList("""
            SELECT
                pe.id_proceso AS "idProceso",
                p.nombres_postulante AS nombres,
                p.apellidos_postulante AS apellidos
            FROM proceso_evaluacion pe
            JOIN postulante p ON pe.id_postulante = p.id_postulante
            WHERE pe.id_solicitud = ?
            ORDER BY p.apellidos_postulante
            """, idSolicitud);
    }
}
