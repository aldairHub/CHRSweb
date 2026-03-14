package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.repository.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final UsuarioRepository           usuarioRepository;
    private final PrepostulacionRepository    prepostulacionRepository;
    private final ConvocatoriaRepository      convocatoriaRepository;
    private final AudCambioRepository         audCambioRepository;
    private final AudLoginAppJpaRepository    loginRepo;
    private final SolicitudDocenteRepository  solicitudDocenteRepository;
    private final ReunionRepository           reunionRepository;
    private final EvaluacionRepository        evaluacionRepository;
    private final ProcesoEvaluacionRepository procesoRepository;

    // ── ADMIN ──────────────────────────────────────────────────────────────
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios",          usuarioRepository.count());
        stats.put("postulantesPendientes",  prepostulacionRepository.findByEstadoRevision("PENDIENTE").size());
        stats.put("convocatoriasAbiertas",  convocatoriaRepository.findByEstadoConvocatoriaOrderByFechaPublicacionDesc("ABIERTA").size());
        stats.put("totalConvocatorias",     convocatoriaRepository.count());
        // Logins exitosos de hoy
        long loginsHoy = loginRepo.findAll().stream()
                .filter(l -> l.getFecha() != null && l.getFecha().toLocalDate().equals(LocalDate.now())
                        && "SUCCESS".equalsIgnoreCase(l.getResultado()))
                .count();
        stats.put("loginsHoy", loginsHoy);
        stats.put("actividadReciente", buildActividad());
        return ResponseEntity.ok(stats);
    }

    // ── EVALUADOR ──────────────────────────────────────────────────────────
    @GetMapping("/evaluador")
    public ResponseEntity<Map<String, Object>> evaluadorStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("solicitudesActivas",      solicitudDocenteRepository.findByEstadoSolicitud("aprobada").size());
        stats.put("entrevistasProgramadas",  reunionRepository.countProgramadas());
        stats.put("evaluacionesCompletadas", evaluacionRepository.countConfirmadas());
        stats.put("procesosActivos",         procesoRepository.countActivos());
        stats.put("actividadReciente",       buildActividad());
        return ResponseEntity.ok(stats);
    }

    // ── REVISOR ────────────────────────────────────────────────────────────
    @GetMapping("/revisor")
    public ResponseEntity<Map<String, Object>> revisorStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("prepostulacionesPendientes", prepostulacionRepository.findByEstadoRevision("PENDIENTE").size());
        stats.put("aprobadas",                  prepostulacionRepository.findByEstadoRevision("APROBADO").size());
        stats.put("rechazadas",                 prepostulacionRepository.findByEstadoRevision("RECHAZADO").size());
        stats.put("convocatoriasActivas",        convocatoriaRepository.findByEstadoConvocatoriaOrderByFechaPublicacionDesc("ABIERTA").size());
        stats.put("actividadReciente",           buildActividad());
        return ResponseEntity.ok(stats);
    }

    // ── POSTULANTE ─────────────────────────────────────────────────────────
    @GetMapping("/postulante")
    public ResponseEntity<Map<String, Object>> postulanteStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("convocatoriasAbiertas", convocatoriaRepository.findByEstadoConvocatoriaOrderByFechaPublicacionDesc("ABIERTA").size());
        stats.put("procesosEnCurso",       procesoRepository.countActivos());
        stats.put("entrevistasHoy",        reunionRepository.countByFecha(LocalDate.now()));
        stats.put("actividadReciente",     buildActividad());
        return ResponseEntity.ok(stats);
    }

    // ── ACTIVIDAD RECIENTE — usa aud_cambio (nueva tabla del proyecto) ─────
    private List<Map<String, Object>> buildActividad() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return audCambioRepository
                .findAll(PageRequest.of(0, 5, Sort.by("fecha").descending()))
                .getContent()
                .stream()
                .map(a -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("descripcion", humanizar(a.getOperacion(), a.getTabla(), a.getCampo(), a.getValorDespues()));
                    item.put("accion",   a.getOperacion());
                    item.put("entidad",  a.getTabla());
                    item.put("usuario",  a.getUsuarioApp() != null ? a.getUsuarioApp() : a.getUsuarioBd());
                    item.put("fecha",    a.getFecha() != null ? a.getFecha().format(fmt) : "");
                    return item;
                })
                .collect(Collectors.toList());
    }

    private String humanizar(String operacion, String tabla, String campo, String valorDespues) {
        String entidad = traducirTabla(tabla);
        String accion  = traducirOperacion(operacion);

        // Si hay un valor nuevo legible y el campo es un nombre/título, mostrarlo
        if (valorDespues != null && !valorDespues.isBlank()
                && (campo != null && (campo.contains("titulo") || campo.contains("nombre") || campo.contains("descripcion")))
                && valorDespues.length() < 60) {
            return accion + " " + entidad + ": " + valorDespues;
        }

        if (campo != null && !campo.isBlank()) {
            return accion + " en " + entidad + " — campo: " + campo.replace("_", " ");
        }

        return accion + " " + entidad;
    }

    private String traducirOperacion(String op) {
        if (op == null) return "Cambio";
        return switch (op.toUpperCase()) {
            case "INSERT" -> "Nuevo registro en";
            case "UPDATE" -> "Actualización en";
            case "DELETE" -> "Eliminación en";
            default       -> op;
        };
    }

    private String traducirTabla(String tabla) {
        if (tabla == null) return "";
        return switch (tabla.toLowerCase()) {
            case "convocatoria"       -> "convocatoria";
            case "solicitud_docente"  -> "solicitud docente";
            case "prepostulacion"     -> "postulación";
            case "usuario"            -> "usuario";
            case "postulante"         -> "postulante";
            case "documento"          -> "documento";
            case "carrera"            -> "carrera";
            case "facultad"           -> "facultad";
            case "rol_app"            -> "rol de aplicación";
            case "config_backup"      -> "configuración de respaldo";
            case "historial_backup"   -> "respaldo";
            case "reunion"            -> "reunión / entrevista";
            case "evaluacion"         -> "evaluación";
            default                   -> tabla.replace("_", " ");
        };
    }
}
