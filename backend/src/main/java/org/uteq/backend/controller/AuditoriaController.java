package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.AudLoginApp;
import org.uteq.backend.repository.AudLoginAppJpaRepository;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.service.AuditoriaService;
import org.uteq.backend.service.JwtService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    private final AuditoriaService              auditoriaService;
    private final AudLoginAppJpaRepository      auditoriaRepository;
    private final PostgresProcedureRepository   procedureRepository;
    private final JwtService                    jwtService;

    public AuditoriaController(AuditoriaService auditoriaService,
                               AudLoginAppJpaRepository auditoriaRepository,
                               PostgresProcedureRepository procedureRepository,
                               JwtService jwtService) {
        this.auditoriaService    = auditoriaService;
        this.auditoriaRepository = auditoriaRepository;
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
}