package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.AudLoginApp;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.service.AuditoriaService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    private final AuditoriaService            auditoriaService;
    private final PostgresProcedureRepository procedureRepository;
    private final JdbcTemplate                jdbc;

    public AuditoriaController(AuditoriaService auditoriaService,
                               PostgresProcedureRepository procedureRepository,
                               JdbcTemplate jdbc) {
        this.auditoriaService    = auditoriaService;
        this.procedureRepository = procedureRepository;
        this.jdbc                = jdbc;
    }

    // ── Auditoría de login ────────────────────────────────────────────────

    @GetMapping("/login")
    public ResponseEntity<Page<AudLoginApp>> listar(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String usuarioApp,
            @RequestParam(required = false)    String resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        return ResponseEntity.ok(
                auditoriaService.buscar(usuarioApp, resultado, desde, hasta, pageable)
        );
    }

    // ── Sesiones activas ─────────────────────────────────────────────────

    @GetMapping("/sesiones/activas")
    public ResponseEntity<List<Map<String, Object>>> listarSesionesActivas() {
        return ResponseEntity.ok(procedureRepository.listarSesionesActivas());
    }

    @PostMapping("/sesiones/{usuarioApp}/forzar-cierre")
    public ResponseEntity<Map<String, String>> forzarCierre(
            @PathVariable String usuarioApp,
            HttpServletRequest request) {
        procedureRepository.invalidarTokenUsuario(usuarioApp);
        procedureRepository.cerrarSesion(usuarioApp, "FORCE_LOGOUT");
        return ResponseEntity.ok(Map.of(
                "mensaje",    "Sesión de '" + usuarioApp + "' invalidada correctamente",
                "usuarioApp", usuarioApp
        ));
    }

    // ── Estadísticas — delegadas a SPs con filtros opcionales ────────────
    /**
     * GET /api/admin/auditoria/estadisticas/login
     * Llama a sp_stats_login(desde, hasta, usuario_app, resultado)
     */
    @GetMapping("/estadisticas/login")
    public ResponseEntity<Map<String, Object>> estadisticasLogin(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuarioApp,
            @RequestParam(required = false) String resultado
    ) {
        String sql = "SELECT * FROM public.sp_stats_login(?, ?, ?, ?)";

        Map<String, Object> resp = jdbc.queryForMap(sql,
                desde      != null ? Date.valueOf(desde)      : null,
                hasta      != null ? Date.valueOf(hasta)      : null,
                blankNull(usuarioApp),
                blankNull(resultado)
        );

        return ResponseEntity.ok(toCamel(resp));
    }

    /**
     * GET /api/admin/auditoria/estadisticas/cambios
     * Llama a sp_stats_cambios(desde, hasta, tabla, operacion, usuario_app)
     */
    @GetMapping("/estadisticas/cambios")
    public ResponseEntity<Map<String, Object>> estadisticasCambios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) String operacion,
            @RequestParam(required = false) String usuarioApp
    ) {
        String sql = "SELECT * FROM public.sp_stats_cambios(?, ?, ?, ?, ?)";

        Map<String, Object> resp = jdbc.queryForMap(sql,
                desde      != null ? Date.valueOf(desde)      : null,
                hasta      != null ? Date.valueOf(hasta)      : null,
                blankNull(tabla),
                blankNull(operacion),
                blankNull(usuarioApp)
        );

        return ResponseEntity.ok(toCamel(resp));
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private String blankNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * Convierte las claves snake_case que devuelve PostgreSQL a camelCase
     * y normaliza valores desconocidos (PGobject, byte[], etc.) a String
     * para que el frontend reciba los arrays JSON como texto parseable.
     */
    private Map<String, Object> toCamel(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(snakeToCamel(k), normalizeValue(v)));
        return result;
    }

    /**
     * Normaliza el valor sin depender de clases específicas de PostgreSQL:
     * - null         → null
     * - String       → String (ya está bien)
     * - Number/Bool  → tal cual (son tipos primitivos serializables)
     * - byte[]       → String UTF-8 (bytea que contiene texto JSON)
     * - cualquier otro objeto (PGobject, etc.) → .toString() que para
     *   PGobject devuelve el valor JSON, y para jsonb el texto plano
     */
    private Object normalizeValue(Object v) {
        if (v == null)             return null;
        if (v instanceof String)   return v;
        if (v instanceof Number)   return v;
        if (v instanceof Boolean)  return v;
        if (v instanceof byte[])   return new String((byte[]) v, java.nio.charset.StandardCharsets.UTF_8);
        // PGobject, PgArray y cualquier tipo desconocido de JDBC/PostgreSQL
        return v.toString();
    }

    private String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean next = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') { next = true; }
            else if (next) { sb.append(Character.toUpperCase(c)); next = false; }
            else { sb.append(c); }
        }
        return sb.toString();
    }
}