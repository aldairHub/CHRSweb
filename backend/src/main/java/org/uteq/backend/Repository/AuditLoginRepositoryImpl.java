package org.uteq.backend.Repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.AuditLoginResultado;

@Repository
public class AuditLoginRepositoryImpl implements IAuditLoginRepository {

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
            String userAgent
    ) {
        jdbcTemplate.queryForObject(
                "SELECT public.fn_auditar_login_app(?, ?, ?, ?, ?::inet, ?)",
                Object.class,
                usuarioApp,
                usuarioBd,
                resultado.name(),
                (motivo == null ? null : motivo.name()),
                ipCliente,
                userAgent
        );
    }
//    public String whoAmI() {
//        return jdbcTemplate.queryForObject(
//                "select current_database() || ' | ' || current_schema() || ' | ' || current_user",
//                String.class
//        );
//    }

}
