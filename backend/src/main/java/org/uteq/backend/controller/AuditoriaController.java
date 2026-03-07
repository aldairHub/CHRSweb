package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.AudLoginApp;
import org.uteq.backend.entity.HistorialAccion;
import org.uteq.backend.repository.AudLoginAppJpaRepository;
import org.uteq.backend.repository.HistorialAccionRepository;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.service.AuditoriaService;
import org.uteq.backend.service.JwtService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    private final AuditoriaService              auditoriaService;
    private final AudLoginAppJpaRepository      auditoriaRepository;
    private final HistorialAccionRepository     historialRepository;
    private final PostgresProcedureRepository   procedureRepository;
    private final JwtService                    jwtService;

    public AuditoriaController(AuditoriaService auditoriaService,
                               AudLoginAppJpaRepository auditoriaRepository,
                               HistorialAccionRepository historialRepository,
                               PostgresProcedureRepository procedureRepository,
                               JwtService jwtService) {
        this.auditoriaService    = auditoriaService;
        this.auditoriaRepository = auditoriaRepository;
        this.historialRepository = historialRepository;
        this.procedureRepository = procedureRepository;
        this.jwtService          = jwtService;
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

    @GetMapping("/login/stats")
    public ResponseEntity<List<Object[]>> stats() {
        return ResponseEntity.ok(auditoriaRepository.statsUltimos7Dias());
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

        // Invalidar token (incrementa token_version en BD)
        procedureRepository.invalidarTokenUsuario(usuarioApp);

        // Cerrar sesión activa
        procedureRepository.cerrarSesion(usuarioApp, "FORCE_LOGOUT");

        return ResponseEntity.ok(Map.of(
                "mensaje",     "Sesión de '" + usuarioApp + "' invalidada correctamente",
                "usuarioApp",  usuarioApp
        ));
    }

    // ── Historial de acciones ─────────────────────────────────────────────

    @GetMapping("/historial")
    public ResponseEntity<Page<HistorialAccion>> listarHistorial(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        LocalDateTime desdeTs = desde != null ? desde.atStartOfDay()     : null;
        LocalDateTime hastaTs = hasta != null ? hasta.atTime(23, 59, 59) : null;
        Page<HistorialAccion> resultado = historialRepository.buscarFiltrado(
                usuario != null && !usuario.isBlank() ? usuario.trim() : null,
                desdeTs, hastaTs, pageable
        );
        return ResponseEntity.ok(resultado);
    }

    // ── Estadísticas de auditoría ─────────────────────────────────────────

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> estadisticas() {
        long totalRegistros = auditoriaRepository.count();
        long totalExitosos  = auditoriaRepository.countByResultado("SUCCESS");
        long totalFallidos  = auditoriaRepository.countByResultado("FAIL");

        List<Object[]> rawStats = auditoriaRepository.statsUltimos7Dias();
        List<Map<String, Object>> tendencia = rawStats.stream().map(row -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("fecha",    row[0]);
            e.put("total",    row[1]);
            e.put("exitosos", row[2]);
            e.put("fallidos", row[3]);
            return e;
        }).collect(Collectors.toList());

        List<Object[]> rawFallidos = auditoriaRepository.top10UsuariosFallidos();
        List<Map<String, Object>> topFallidos = rawFallidos.stream().map(row -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("usuario",  row[0]);
            e.put("intentos", row[1]);
            return e;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalRegistros",        totalRegistros);
        resp.put("totalExitosos",         totalExitosos);
        resp.put("totalFallidos",         totalFallidos);
        resp.put("tasaExito",             totalRegistros > 0
                ? Math.round((totalExitosos * 100.0) / totalRegistros * 10) / 10.0 : 0);
        resp.put("tendenciaSemanal",      tendencia);
        resp.put("topFallidosPorUsuario", topFallidos);

        return ResponseEntity.ok(resp);
    }

}