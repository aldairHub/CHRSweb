package org.uteq.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.IntegridadRegistroDTO;
import org.uteq.backend.dto.IntegridadResumenDTO;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IntegridadAuditoriaService {

    private final JdbcTemplate jdbc;

    public IntegridadAuditoriaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Llama a fn_verificar_integridad(p_limite) y construye el resumen.
     */
    public IntegridadResumenDTO verificar(int limite) {

        String sql = "SELECT * FROM public.fn_verificar_integridad(?)";

        List<Map<String, Object>> rows = jdbc.queryForList(sql, limite);

        List<IntegridadRegistroDTO> todos       = new ArrayList<>();
        List<IntegridadRegistroDTO> sospechosos = new ArrayList<>();
        int totalOk       = 0;
        int totalAlterado = 0;
        int totalSinHash  = 0;

        for (Map<String, Object> row : rows) {
            IntegridadRegistroDTO dto = mapRow(row);
            todos.add(dto);

            switch (dto.getEstado()) {
                case "OK"       -> totalOk++;
                case "ALTERADO" -> { totalAlterado++; sospechosos.add(dto); }
                case "SIN_HASH" -> { totalSinHash++;  sospechosos.add(dto); }
            }
        }

        IntegridadResumenDTO resumen = new IntegridadResumenDTO();
        resumen.setTotalVerificados(todos.size());
        resumen.setTotalOk(totalOk);
        resumen.setTotalAlterados(totalAlterado);
        resumen.setTotalSinHash(totalSinHash);
        resumen.setRegistrosSospechosos(sospechosos);
        return resumen;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private IntegridadRegistroDTO mapRow(Map<String, Object> row) {
        IntegridadRegistroDTO dto = new IntegridadRegistroDTO();
        dto.setIdAudCambio(toLong(row.get("id_aud_cambio")));
        dto.setTabla(toString(row.get("tabla")));
        dto.setOperacion(toString(row.get("operacion")));
        dto.setCampo(toString(row.get("campo")));
        dto.setUsuarioApp(toString(row.get("usuario_app")));
        dto.setHashGuardado(toString(row.get("hash_guardado")));
        dto.setHashCalculado(toString(row.get("hash_calculado")));
        dto.setEstado(toString(row.get("estado")));

        Object fechaObj = row.get("fecha");
        if (fechaObj instanceof Timestamp ts) {
            dto.setFecha(ts.toInstant().atOffset(ZoneOffset.UTC));
        } else if (fechaObj instanceof OffsetDateTime odt) {
            dto.setFecha(odt);
        }
        return dto;
    }

    private Long toLong(Object o)     { return o == null ? null : ((Number) o).longValue(); }
    private String toString(Object o) { return o == null ? null : o.toString(); }
}
