package org.uteq.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.uteq.backend.entity.AudLoginApp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditoriaService {

    private final JdbcTemplate jdbc;

    public AuditoriaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Page<AudLoginApp> buscar(
            String usuarioApp,
            String resultado,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (usuarioApp != null && !usuarioApp.isBlank()) {
            where.append(" AND LOWER(usuario_app) LIKE LOWER(?) ");
            params.add("%" + usuarioApp.trim() + "%");
        }
        if (resultado != null && !resultado.isBlank()) {
            where.append(" AND resultado = ? ");
            params.add(resultado.trim());
        }
        if (desde != null) {
            where.append(" AND fecha >= ? ");
            params.add(desde.atStartOfDay());
        }
        if (hasta != null) {
            where.append(" AND fecha < ? ");
            params.add(hasta.plusDays(1).atStartOfDay());
        }

        // Contar total
        String countSql = "SELECT COUNT(*) FROM public.aud_login_app" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());
        if (total == null) total = 0L;

        // Obtener página
        String dataSql = "SELECT id_aud, fecha, ip_cliente, motivo, resultado, user_agent, usuario_app, usuario_bd"
                + " FROM public.aud_login_app"
                + where
                + " ORDER BY fecha DESC"
                + " LIMIT ? OFFSET ?";

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageable.getPageSize());
        dataParams.add(pageable.getOffset());

        List<AudLoginApp> rows = jdbc.query(dataSql, new AudLoginAppRowMapper(), dataParams.toArray());

        return new PageImpl<>(rows, pageable, total);
    }

    private static class AudLoginAppRowMapper implements RowMapper<AudLoginApp> {
        @Override
        public AudLoginApp mapRow(ResultSet rs, int rowNum) throws SQLException {
            AudLoginApp a = new AudLoginApp();
            a.setIdAud(rs.getLong("id_aud"));
            a.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
            a.setIpCliente(rs.getString("ip_cliente"));
            a.setMotivo(rs.getString("motivo"));
            a.setResultado(rs.getString("resultado"));
            a.setUserAgent(rs.getString("user_agent"));
            a.setUsuarioApp(rs.getString("usuario_app"));
            a.setUsuarioBd(rs.getString("usuario_bd"));
            return a;
        }
    }
}