package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.repository.ConvocatoriaRepository;
import org.uteq.backend.repository.PrepostulacionRepository;
import org.uteq.backend.repository.UsuarioRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final UsuarioRepository          usuarioRepository;
    private final PrepostulacionRepository   prepostulacionRepository;
    private final ConvocatoriaRepository     convocatoriaRepository;
    private final JdbcTemplate               jdbc;

    @GetMapping("/admin-stats")
    public ResponseEntity<Map<String, Object>> adminStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("usuariosActivos",      usuarioRepository.count());
        stats.put("postulantesPendientes",
                prepostulacionRepository.findByEstadoRevision("PENDIENTE").size());
        stats.put("convocatoriasAbiertas",
                convocatoriaRepository.findByEstadoConvocatoriaOrderByFechaPublicacionDesc("ABIERTA").size());
        stats.put("totalConvocatorias",   convocatoriaRepository.count());

        // Actividad reciente — últimos 5 cambios de aud_cambio
        // Sin SELECT directo: usa SP de datos de cambios con límite 5
        List<Map<String, Object>> actividad = jdbc.queryForList("""
                SELECT
                    tabla,
                    operacion,
                    campo,
                    COALESCE(usuario_app, '(externo)') AS usuario,
                    TO_CHAR(fecha AT TIME ZONE 'America/Guayaquil', 'YYYY-MM-DD HH24:MI') AS fecha
                FROM public.sp_reporte_cambios(NULL, NULL, NULL, NULL, NULL, 5)
                ORDER BY fecha DESC
                """);

        stats.put("actividadReciente", actividad);

        return ResponseEntity.ok(stats);
    }
}