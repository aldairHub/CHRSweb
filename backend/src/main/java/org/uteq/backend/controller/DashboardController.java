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

        // Agrupar para evitar duplicados:
        // Clave = usuario + operacion + tabla + minuto exacto
        // Así colapsa: múltiples campos del mismo registro Y múltiples registros creados en ráfaga
        Map<String, AudCambio> agrupados = new LinkedHashMap<>();

        audCambioRepository
                .findAll(PageRequest.of(0, 500, Sort.by("fecha").descending()))
                .getContent()
                .stream()
                // Solo actividades de hoy
                .filter(a -> a.getFecha() != null
                        && !a.getFecha().isBefore(inicioDia)
                        && !a.getFecha().isAfter(finDia))
                // Solo tablas relevantes al rol
                .filter(a -> a.getTabla() != null
                        && tablasPermitidas.contains(a.getTabla().toLowerCase().trim()))
                // Ignorar campos técnicos
                .filter(a -> {
                    if (a.getCampo() == null) return true;
                    String c = a.getCampo().toLowerCase().trim();
                    return !CAMPOS_IGNORAR.contains(c);
                })
                // Solo del usuario si es postulante
                .filter(a -> soloUsuario == null || soloUsuario.isBlank()
                        || soloUsuario.equalsIgnoreCase(a.getUsuarioApp()))
                .forEach(a -> {
                    String usuario = a.getUsuarioApp() != null ? a.getUsuarioApp() : a.getUsuarioBd();
                    // Agrupar por: usuario + operacion + tabla + minuto (HH:mm)
                    // Esto colapsa tanto múltiples campos del mismo registro
                    // como múltiples registros creados en el mismo minuto por el mismo usuario
                    String minuto = a.getFecha() != null
                            ? a.getFecha().format(DateTimeFormatter.ofPattern("HH:mm"))
                            : "00:00";
                    String clave = usuario + "|" + a.getOperacion() + "|" + a.getTabla() + "|" + minuto;

                    if (!agrupados.containsKey(clave)) {
                        agrupados.put(clave, a);
                    } else {
                        // Preferir el registro con campo más descriptivo
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
        String tabla = traducirTabla(a.getTabla());
        String op    = a.getOperacion();
        String campo = a.getCampo();
        String valor = a.getValorDespues();

        if ("INSERT".equalsIgnoreCase(op)) {
            // Mensajes específicos por tabla en INSERT
            return switch (a.getTabla() != null ? a.getTabla().toLowerCase() : "") {
                case "usuario"              -> "Nuevo usuario registrado en el sistema";
                case "convocatoria"         -> valor != null && campo != null && campo.contains("titulo") && valor.length() < 80
                        ? "Nueva convocatoria publicada: \"" + valor + "\""
                        : "Nueva convocatoria creada";
                case "solicitud_docente"    -> "Nueva solicitud docente enviada";
                case "prepostulacion"       -> "Nueva postulación recibida";
                case "reunion"              -> "Nueva entrevista programada";
                case "evaluacion"           -> "Nueva evaluación registrada";
                case "proceso_evaluacion"   -> "Nuevo proceso de evaluación iniciado";
                case "carrera"              -> valor != null && campo != null && campo.contains("nombre") && valor.length() < 60
                        ? "Nueva carrera registrada: \"" + valor + "\""
                        : "Nueva carrera registrada";
                case "facultad"             -> valor != null && campo != null && campo.contains("nombre") && valor.length() < 60
                        ? "Nueva facultad registrada: \"" + valor + "\""
                        : "Nueva facultad registrada";
                case "documento_postulante" -> "Nuevo documento subido";
                case "historial_backup"     -> "Respaldo de base de datos ejecutado";
                case "config_institucion"   -> "Configuración institucional actualizada";
                case "fase_evaluacion"      -> "Nueva fase de evaluación configurada";
                case "plantilla_evaluacion" -> "Nueva plantilla de evaluación creada";
                case "criterio_evaluacion"  -> "Nuevo criterio de evaluación agregado";
                default                     -> "Nuevo " + tabla + " registrado";
            };
        }

        if ("DELETE".equalsIgnoreCase(op)) {
            return switch (a.getTabla() != null ? a.getTabla().toLowerCase() : "") {
                case "usuario"              -> "Usuario eliminado del sistema";
                case "convocatoria"         -> "Convocatoria eliminada";
                case "solicitud_docente"    -> "Solicitud docente eliminada";
                case "reunion"              -> "Entrevista cancelada y eliminada";
                case "documento_postulante" -> "Documento eliminado";
                case "fase_evaluacion"      -> "Fase de evaluación eliminada";
                case "criterio_evaluacion"  -> "Criterio de evaluación eliminado";
                default                     -> tabla.substring(0, 1).toUpperCase() + tabla.substring(1) + " eliminado";
            };
        }

        if ("UPDATE".equalsIgnoreCase(op)) {
            return describeCambio(a.getTabla(), tabla, campo, valor);
        }

        return "Cambio en " + tabla;
    }

    private String describeCambio(String rawTabla, String tabla, String campo, String valorDespues) {
        if (campo == null) return "Se actualizó " + tabla;
        String raw = rawTabla != null ? rawTabla.toLowerCase() : "";

        // Casos especiales por tabla — más descriptivos
        if ("proceso_evaluacion".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "estado"              -> {
                    String est = valorDespues != null ? valorDespues.toLowerCase() : "";
                    yield switch (est) {
                        case "completado" -> "Proceso de evaluación finalizado";
                        case "en_proceso" -> "Proceso de evaluación en curso";
                        case "cancelado"  -> "Proceso de evaluación cancelado";
                        default           -> "Estado de proceso de evaluación actualizado";
                    };
                }
                case "puntaje_final", "calificacion_final" ->
                        "Calificación final registrada en proceso de evaluación";
                default -> "Proceso de evaluación actualizado";
            };
        }

        if ("reunion".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "estado_reunion", "estado" -> {
                    String est = valorDespues != null ? valorDespues.toLowerCase() : "";
                    yield switch (est) {
                        case "completada"  -> "Entrevista completada exitosamente";
                        case "cancelada"   -> "Entrevista cancelada";
                        case "programada"  -> "Entrevista reprogramada";
                        default            -> "Estado de entrevista actualizado";
                    };
                }
                case "fecha_reunion"  -> "Fecha de entrevista actualizada";
                case "hora_reunion"   -> "Hora de entrevista actualizada";
                case "enlace", "enlace_reunion" -> "Enlace de entrevista actualizado";
                default -> "Entrevista actualizada";
            };
        }

        if ("convocatoria".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "estado_convocatoria" -> {
                    String est = valorDespues != null ? valorDespues.toUpperCase() : "";
                    yield switch (est) {
                        case "ABIERTA"   -> "Convocatoria abierta al público";
                        case "CERRADA"   -> "Convocatoria cerrada";
                        case "PUBLICADA" -> "Convocatoria publicada";
                        case "BORRADOR"  -> "Convocatoria guardada como borrador";
                        default          -> "Estado de convocatoria actualizado";
                    };
                }
                case "titulo_convocatoria", "titulo" ->
                        valorDespues != null && valorDespues.length() < 80
                                ? "Título de convocatoria actualizado: \"" + valorDespues + "\""
                                : "Título de convocatoria actualizado";
                case "fecha_inicio" -> "Fecha de inicio de convocatoria modificada";
                case "fecha_fin"    -> "Fecha de cierre de convocatoria modificada";
                default -> "Convocatoria actualizada";
            };
        }

        if ("prepostulacion".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "estado_revision" -> {
                    String est = valorDespues != null ? valorDespues.toUpperCase() : "";
                    yield switch (est) {
                        case "APROBADO"  -> "Postulación aprobada por el revisor";
                        case "RECHAZADO" -> "Postulación rechazada por el revisor";
                        case "PENDIENTE" -> "Postulación en revisión";
                        default          -> "Estado de postulación actualizado";
                    };
                }
                default -> "Postulación actualizada";
            };
        }

        if ("solicitud_docente".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "estado_solicitud" -> {
                    String est = valorDespues != null ? valorDespues.toLowerCase() : "";
                    yield switch (est) {
                        case "aprobada"  -> "Solicitud docente aprobada";
                        case "rechazada" -> "Solicitud docente rechazada";
                        case "pendiente" -> "Solicitud docente enviada a revisión";
                        default          -> "Estado de solicitud docente actualizado";
                    };
                }
                default -> "Solicitud docente actualizada";
            };
        }

        if ("documento_postulante".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "estado_documento" -> {
                    String est = valorDespues != null ? valorDespues.toUpperCase() : "";
                    yield switch (est) {
                        case "APROBADO"  -> "Documento aprobado";
                        case "RECHAZADO" -> "Documento rechazado";
                        default          -> "Estado de documento actualizado";
                    };
                }
                case "ruta_archivo", "nombre_archivo" -> "Documento actualizado";
                default -> "Documento actualizado";
            };
        }

        if ("usuario".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "estado", "activo"   -> "true".equalsIgnoreCase(valorDespues) || "1".equals(valorDespues)
                        ? "Usuario activado en el sistema"
                        : "Usuario desactivado en el sistema";
                case "correo", "email"    -> "Correo electrónico de usuario actualizado";
                case "rol", "id_rol_app"  -> "Rol de usuario modificado";
                case "primer_login"       -> "Usuario completó su configuración inicial";
                case "nombres", "apellidos", "nombre_usuario" -> "Datos personales de usuario actualizados";
                default -> "Información de usuario actualizada";
            };
        }

        if ("evaluacion".equals(raw)) {
            return switch (campo.toLowerCase()) {
                case "puntaje", "calificacion", "nota" -> "Calificación registrada en evaluación";
                case "estado_evaluacion" -> "evaluada".equalsIgnoreCase(valorDespues)
                        ? "Evaluación completada" : "Evaluación actualizada";
                case "comentarios", "observaciones" -> "Comentarios de evaluación actualizados";
                default -> "Evaluación actualizada";
            };
        }

        // Fallback genérico para tablas no cubiertas arriba
        return tabla.substring(0, 1).toUpperCase() + tabla.substring(1) + " actualizado";
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