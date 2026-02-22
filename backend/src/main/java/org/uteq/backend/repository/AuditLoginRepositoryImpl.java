package org.uteq.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.AuditLoginResultado;

@Repository
public class AuditLoginRepositoryImpl implements AuditLoginRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLoginRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void auditLogin(
            String usuarioApp,
            String usuarioBd,
            AuditLoginResultado resultado,
            AuditLoginMotivo motivo,
            String ipCliente,
            String userAgent,
            Long idUsuario  // ✅ nuevo
    ) {
        jdbcTemplate.queryForObject(
                "SELECT public.fn_auditar_login_app(?, ?, ?, ?, ?::inet, ?, ?)",
                Object.class,
                usuarioApp,
                usuarioBd,
                resultado.name(),
                (motivo == null ? null : motivo.name()),
                ipCliente,
                userAgent,
                idUsuario
        );
    }

}
