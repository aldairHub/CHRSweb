package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.AudCambio;
import org.uteq.backend.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    // Tablas relevantes por rol
    private static final Set<String> TABLAS_ADMIN = Set.of(
            "usuario", "rol_app", "carrera", "facultad", "materia",
            "config_backup", "historial_backup", "config_institucion",
            "modulo", "opcion", "tipo_documento", "nivel"
    );
    private static final Set<String> TABLAS_REVISOR = Set.of(
            "convocatoria", "solicitud_docente", "prepostulacion",
            "documento_postulante", "tipo_documento"
    );
    private static final Set<String> TABLAS_EVALUADOR = Set.of(
            "reunion", "evaluacion", "proceso_evaluacion",
            "fase_evaluacion", "plantilla_evaluacion", "criterio_evaluacion",
            "resultado_evaluacion", "solicitud_docente"
    );
    private static final Set<String> TABLAS_POSTULANTE = Set.of(
            "prepostulacion", "documento_postulante", "proceso_evaluacion",
            "reunion", "evaluacion"
    );

    // Campos técnicos que NO deben mostrarse como actividad
    private static final Set<String> CAMPOS_IGNORAR = Set.of(
            "token_version", "token", "clave_bd", "password",
            "ultimo_acceso", "fecha_modificacion", "updated_at"
    );

    // ── ADMIN ──────────────────────────────────────────────────────────────
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios",         usuarioRepository.count());
        stats.put("postulantesPendientes", prepostulacionRepository.findByEstadoRevision("PENDIENTE").size());
        stats.put("convocatoriasAbiertas", convocatoriaRepository.findByEstadoConvocatoriaOrderByFechaPublicacionDesc("ABIERTA").size());
        stats.put("totalConvocatorias",    convocatoriaRepository.count());
        long loginsHoy = loginRepo.findAll().stream()
                .filter(l -> l.getFecha() != null
                        && l.getFecha().toLocalDate().equals(LocalDate.now())
                        && "SUCCESS".equalsIgnoreCase(l.getResultado()))
                .count();
        stats.put("loginsHoy", loginsHoy);
        stats.put("actividadReciente", buildActividad(TABLAS_ADMIN, null));
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
        stats.put("actividadReciente",       buildActividad(TABLAS_EVALUADOR, null));
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
        stats.put("actividadReciente",           buildActividad(TABLAS_REVISOR, null));
        return ResponseEntity.ok(stats);
    }

    // ── POSTULANTE — solo sus propias actividades ─────────────────────────
    @GetMapping("/postulante")
    public ResponseEntity<Map<String, Object>> postulanteStats(
            @RequestParam(required = false) String usuarioApp) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("convocatoriasAbiertas", convocatoriaRepository.findByEstadoConvocatoriaOrderByFechaPublicacionDesc("ABIERTA").size());
        stats.put("procesosEnCurso",       procesoRepository.countActivos());
        stats.put("entrevistasHoy",        reunionRepository.countByFecha(LocalDate.now()));
        // El postulante solo ve sus propias acciones
        stats.put("actividadReciente",     buildActividad(TABLAS_POSTULANTE, usuarioApp));
        return ResponseEntity.ok(stats);
    }

    // ── BUILDER — filtra por tablas del rol + solo hoy + sin campos técnicos
    private List<Map<String, Object>> buildActividad(Set<String> tablasPermitidas, String soloUsuario) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Rango de hoy: desde 00:00 hasta 23:59:59 en UTC
        OffsetDateTime inicioDia = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime finDia    = LocalDate.now().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        // Agrupar por operacion+tabla+idRegistro para evitar duplicados
        // (la auditoría guarda un registro por cada campo cambiado)
        Map<String, AudCambio> agrupados = new LinkedHashMap<>();

        audCambioRepository
                .findAll(PageRequest.of(0, 200, Sort.by("fecha").descending()))
                .getContent()
                .stream()
                // Solo actividades de hoy
                .filter(a -> a.getFecha() != null
                        && !a.getFecha().isBefore(inicioDia)
                        && !a.getFecha().isAfter(finDia))
                // Solo tablas relevantes al rol
                .filter(a -> a.getTabla() != null
                        && tablasPermitidas.contains(a.getTabla().toLowerCase().trim()))
                // Ignorar campos técnicos (token_version, password, etc)
                .filter(a -> {
                    if (a.getCampo() == null) return true;
                    String c = a.getCampo().toLowerCase().trim();
                    return !CAMPOS_IGNORAR.contains(c);
                })
                // Solo del usuario si es postulante
                .filter(a -> soloUsuario == null || soloUsuario.isBlank()
                        || soloUsuario.equalsIgnoreCase(a.getUsuarioApp()))
                .forEach(a -> {
                    // Clave única: operacion + tabla + idRegistro
                    String clave = a.getOperacion() + "|" + a.getTabla() + "|" + a.getIdRegistro();
                    // Guardar solo el primer registro de cada grupo (el más reciente)
                    // Preferir el que tenga un campo con nombre/titulo/estado (más descriptivo)
                    if (!agrupados.containsKey(clave)) {
                        agrupados.put(clave, a);
                    } else {
                        // Si el nuevo tiene un campo más descriptivo, reemplazar
                        AudCambio actual = agrupados.get(clave);
                        if (esCampoDescriptivo(a.getCampo()) && !esCampoDescriptivo(actual.getCampo())) {
                            agrupados.put(clave, a);
                        }
                    }
                });

        return agrupados.values().stream()
                .limit(10)
                .map(a -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("descripcion", humanizar(a));
                    item.put("accion",   a.getOperacion());
                    item.put("entidad",  a.getTabla());
                    item.put("usuario",  a.getUsuarioApp() != null ? a.getUsuarioApp() : a.getUsuarioBd());
                    item.put("fecha",    a.getFecha() != null ? a.getFecha().format(fmt) : "");
                    return item;
                })
                .collect(Collectors.toList());
    }

    private boolean esCampoDescriptivo(String campo) {
        if (campo == null) return false;
        String c = campo.toLowerCase();
        return c.contains("nombre") || c.contains("titulo") || c.contains("estado")
                || c.contains("descripcion") || c.contains("correo") || c.contains("email");
    }

    private String humanizar(AudCambio a) {
        String tabla  = traducirTabla(a.getTabla());
        String op     = a.getOperacion();
        String campo  = a.getCampo();
        String valor  = a.getValorDespues();

        // INSERT — nuevo registro
        if ("INSERT".equalsIgnoreCase(op)) {
            if (valor != null && !valor.isBlank() && valor.length() < 80
                    && campo != null && (campo.contains("nombre") || campo.contains("titulo") || campo.contains("descripcion"))) {
                return "Se creó " + tabla + ": \"" + valor + "\"";
            }
            return "Se creó un nuevo " + tabla;
        }

        // DELETE
        if ("DELETE".equalsIgnoreCase(op)) {
            return "Se eliminó un " + tabla;
        }

        // UPDATE — describir el cambio en lenguaje natural
        if ("UPDATE".equalsIgnoreCase(op)) {
            return describeCambio(tabla, campo, valor);
        }

        return "Cambio en " + tabla;
    }

    private String describeCambio(String tabla, String campo, String valorDespues) {
        if (campo == null) return "Se actualizó " + tabla;

        return switch (campo.toLowerCase()) {
            // Usuario
            case "estado"                   -> {
                boolean enc = "true".equalsIgnoreCase(valorDespues) || "1".equals(valorDespues) || "activo".equalsIgnoreCase(valorDespues);
                yield (enc ? "Se activó " : "Se desactivó ") + tabla;
            }
            case "nombre_usuario", "nombre_completo", "nombres" ->
                    "Se actualizó el nombre en " + tabla;
            case "correo", "email"          -> "Se cambió el correo de " + tabla;
            case "rol", "rol_app", "id_rol" -> "Se cambió el rol de " + tabla;
            case "primer_login"             -> "Completó configuración inicial";

            // Convocatoria
            case "estado_convocatoria"      -> {
                String est = valorDespues != null ? valorDespues.toUpperCase() : "";
                yield switch (est) {
                    case "ABIERTA"   -> "Se abrió una convocatoria";
                    case "CERRADA"   -> "Se cerró una convocatoria";
                    case "PUBLICADA" -> "Se publicó una convocatoria";
                    default          -> "Se actualizó el estado de convocatoria";
                };
            }
            case "titulo_convocatoria", "titulo" ->
                    "Se actualizó el título de " + tabla + (valorDespues != null && valorDespues.length() < 60 ? ": \"" + valorDespues + "\"" : "");
            case "fecha_inicio", "fecha_fin" ->
                    "Se actualizó la fecha de " + tabla;

            // Solicitud docente
            case "estado_solicitud"         -> {
                String est = valorDespues != null ? valorDespues.toLowerCase() : "";
                yield switch (est) {
                    case "aprobada"  -> "Se aprobó una solicitud docente";
                    case "rechazada" -> "Se rechazó una solicitud docente";
                    case "pendiente" -> "Nueva solicitud docente en revisión";
                    default          -> "Se actualizó el estado de solicitud docente";
                };
            }

            // Postulación
            case "estado_revision"          -> {
                String est = valorDespues != null ? valorDespues.toUpperCase() : "";
                yield switch (est) {
                    case "APROBADO"  -> "Se aprobó una postulación";
                    case "RECHAZADO" -> "Se rechazó una postulación";
                    case "PENDIENTE" -> "Nueva postulación recibida";
                    default          -> "Se actualizó el estado de postulación";
                };
            }

            // Reunión / entrevista
            case "estado_reunion"           -> {
                if (tabla.contains("reunión")) {
                    String est = valorDespues != null ? valorDespues.toLowerCase() : "";
                    yield switch (est) {
                        case "programada"  -> "Se programó una entrevista";
                        case "completada"  -> "Se completó una entrevista";
                        case "cancelada"   -> "Se canceló una entrevista";
                        default            -> "Se actualizó una entrevista";
                    };
                }
                yield "Se actualizó " + tabla;
            }
            case "fecha_reunion", "hora_reunion" ->
                    "Se reprogramó una entrevista";
            case "enlace_reunion", "enlace"      ->
                    "Se actualizó el enlace de entrevista";

            // Evaluación
            case "puntaje", "calificacion", "nota" ->
                    "Se registró una calificación en " + tabla;
            case "estado_evaluacion"        -> {
                String est = valorDespues != null ? valorDespues.toLowerCase() : "";
                yield "evaluada".equals(est) ? "Se completó una evaluación" : "Se actualizó una evaluación";
            }

            // Documento
            case "estado_documento"         -> {
                String est = valorDespues != null ? valorDespues.toUpperCase() : "";
                yield switch (est) {
                    case "APROBADO"  -> "Se aprobó un documento";
                    case "RECHAZADO" -> "Se rechazó un documento";
                    default          -> "Se actualizó un documento";
                };
            }
            case "ruta_archivo", "nombre_archivo" ->
                    "Se subió un documento";

            // Backup
            case "destino_local", "destino_email" ->
                    "Se actualizó la configuración de destino del respaldo";

            // Carrera / facultad
            case "nombre_carrera"           ->
                    "Se actualizó la carrera" + (valorDespues != null && valorDespues.length() < 60 ? ": \"" + valorDespues + "\"" : "");
            case "nombre_facultad"          ->
                    "Se actualizó la facultad" + (valorDespues != null && valorDespues.length() < 60 ? ": \"" + valorDespues + "\"" : "");

            default -> "Se actualizó " + tabla;
        };
    }

    private String traducirTabla(String tabla) {
        if (tabla == null) return "";
        return switch (tabla.toLowerCase()) {
            case "convocatoria"          -> "convocatoria";
            case "solicitud_docente"     -> "solicitud docente";
            case "prepostulacion"        -> "postulación";
            case "usuario"               -> "usuario";
            case "postulante"            -> "postulante";
            case "documento_postulante"  -> "documento";
            case "carrera"               -> "carrera";
            case "facultad"              -> "facultad";
            case "rol_app"               -> "rol de aplicación";
            case "config_backup"         -> "configuración de respaldo";
            case "historial_backup"      -> "respaldo";
            case "reunion"               -> "reunión / entrevista";
            case "evaluacion"            -> "evaluación";
            case "proceso_evaluacion"    -> "proceso de evaluación";
            case "fase_evaluacion"       -> "fase de evaluación";
            case "plantilla_evaluacion"  -> "plantilla de evaluación";
            case "criterio_evaluacion"   -> "criterio de evaluación";
            case "resultado_evaluacion"  -> "resultado de evaluación";
            case "config_institucion"    -> "configuración institucional";
            case "materia"               -> "materia";
            case "nivel"                 -> "nivel académico";
            case "tipo_documento"        -> "tipo de documento";
            default                      -> tabla.replace("_", " ");
        };
    }
}