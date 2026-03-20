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
}
