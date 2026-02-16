package org.uteq.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DbRoleSyncService {

    private final JdbcTemplate jdbc;

    public DbRoleSyncService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void syncRolesUsuarioBd(Integer idUsuario, boolean revocarSobrantes) {
        jdbc.update("CALL public.sp_sync_roles_usuario_bd(?, ?)", idUsuario, revocarSobrantes);
    }
}
