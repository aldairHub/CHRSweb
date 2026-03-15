package org.uteq.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.MonitorResumenDTO;
import org.uteq.backend.dto.QueryMonitorDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MonitorQueriesService {

    private final JdbcTemplate jdbc;

    public MonitorQueriesService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Llama a sp_monitor_queries(p_orden, p_limite).
     */
    public MonitorResumenDTO obtener(String orden, int limite) {
        String sql = "SELECT * FROM public.sp_monitor_queries(?, ?)";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orden, limite);

        MonitorResumenDTO resumen = new MonitorResumenDTO();
        List<QueryMonitorDTO> queries = new ArrayList<>();

        // Detectar si la extensión no está disponible
        if (!rows.isEmpty()) {
            String primerQuery = toString(rows.get(0).get("query_texto"));
            if ("EXTENSION_NO_DISPONIBLE".equals(primerQuery)) {
                resumen.setExtensionDisponible(false);
                resumen.setQueries(queries);
                return resumen;
            }
        }

        resumen.setExtensionDisponible(true);

        for (Map<String, Object> row : rows) {
            QueryMonitorDTO dto = mapRow(row);
            queries.add(dto);
        }

        resumen.setTotalQueriesUnicas(queries.size());
        resumen.setQueries(queries);

        // Query más lenta (por tiempo_promedio)
        queries.stream()
                .max((a, b) -> Double.compare(
                        a.getTiempoPromedioMs() != null ? a.getTiempoPromedioMs() : 0,
                        b.getTiempoPromedioMs() != null ? b.getTiempoPromedioMs() : 0))
                .ifPresent(q -> {
                    resumen.setQueryMasLenta(q.getQueryTexto());
                    resumen.setTiempoMasLento(q.getTiempoPromedioMs() != null ? q.getTiempoPromedioMs() : 0);
                });

        // Query más frecuente
        queries.stream()
                .max((a, b) -> Long.compare(
                        a.getLlamadas() != null ? a.getLlamadas() : 0,
                        b.getLlamadas() != null ? b.getLlamadas() : 0))
                .ifPresent(q -> {
                    resumen.setQueryMasFrecuente(q.getQueryTexto());
                    resumen.setLlamadasMasFrecuente(q.getLlamadas() != null ? q.getLlamadas() : 0);
                });

        return resumen;
    }

    /**
     * Llama a sp_resetear_queries().
     */
    public void resetear() {
        jdbc.execute("SELECT public.sp_resetear_queries()");
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private QueryMonitorDTO mapRow(Map<String, Object> row) {
        QueryMonitorDTO dto = new QueryMonitorDTO();
        dto.setQueryTexto(toString(row.get("query_texto")));
        dto.setLlamadas(toLong(row.get("llamadas")));
        dto.setTiempoPromedioMs(toDouble(row.get("tiempo_promedio_ms")));
        dto.setTiempoTotalMs(toDouble(row.get("tiempo_total_ms")));
        dto.setFilasPromedio(toDouble(row.get("filas_promedio")));
        dto.setPorcentajeTiempo(toDouble(row.get("porcentaje_tiempo")));
        return dto;
    }

    private Long   toLong(Object o)   { return o == null ? null : ((Number) o).longValue(); }
    private Double toDouble(Object o) { return o == null ? null : ((Number) o).doubleValue(); }
    private String toString(Object o) { return o == null ? null : o.toString(); }
}
