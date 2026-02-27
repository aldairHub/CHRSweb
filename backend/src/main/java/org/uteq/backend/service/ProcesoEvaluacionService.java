package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.DecisionRequestDTO;
import org.uteq.backend.dto.PostulanteDetalleDTO;
import org.uteq.backend.dto.PostulanteEvaluacionDTO;
import org.uteq.backend.entity.*;
import org.uteq.backend.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcesoEvaluacionService {

    private final ProcesoEvaluacionRepository procesoRepository;
    private final FaseProcesoRepository faseProcesoRepository;
    private final FaseEvaluacionRepository faseRepository;
    private final ReunionRepository reunionRepository;
    private final HistorialAccionRepository historialRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── Listar todos los postulantes en proceso ───────────────
    @Transactional(readOnly = true)
    public List<PostulanteEvaluacionDTO> listarPostulantes() {
        return procesoRepository.findAllByOrderByFechaInicioDesc()
                .stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());
    }

    // ─── Obtener detalle de un postulante ──────────────────────
    @Transactional(readOnly = true)
    public PostulanteDetalleDTO obtenerDetalle(Long idProceso) {
        ProcesoEvaluacion proceso = findOrThrow(idProceso);

        List<FaseProceso> fasesProceso = faseProcesoRepository
                .findByProceso_IdProcesoOrderByFase_OrdenAsc(idProceso);

        List<PostulanteDetalleDTO.FaseProcesoDTO> fasesDTO = fasesProceso.stream()
                .map(fp -> {
                    PostulanteDetalleDTO.FaseProcesoDTO fDto =
                            PostulanteDetalleDTO.FaseProcesoDTO.builder()
                                    .idFase(fp.getFase().getIdFase())
                                    .orden(fp.getFase().getOrden())
                                    .nombre(fp.getFase().getNombre())
                                    .peso(fp.getFase().getPeso())
                                    .estado(fp.getEstado())
                                    .calificacion(fp.getCalificacion())
                                    .fechaCompletada(fp.getFechaCompletada() != null
                                            ? fp.getFechaCompletada().format(D_FMT) : null)
                                    .evaluadores(stringALista(fp.getEvaluadoresAsignados()))
                                    .build();

                    // Si hay reunión para esta fase-proceso, incluirla
                    reunionRepository.findByFaseProceso_IdFaseProceso(fp.getIdFaseProceso())
                            .ifPresent(r -> fDto.setReunion(toReunionResumen(r, idProceso)));

                    return fDto;
                })
                .collect(Collectors.toList());

        List<PostulanteDetalleDTO.HistorialAccionDTO> historialDTO =
                historialRepository.findByProceso_IdProcesoOrderByFechaDesc(idProceso)
                        .stream()
                        .map(h -> PostulanteDetalleDTO.HistorialAccionDTO.builder()
                                .fecha(h.getFecha().format(DT_FMT))
                                .titulo(h.getTitulo())
                                .descripcion(h.getDescripcion())
                                .usuario(h.getUsuario())
                                .build())
                        .collect(Collectors.toList());

        Postulante postulante = proceso.getPostulante();

        return PostulanteDetalleDTO.builder()
                .idPostulante(idProceso)
                .codigo(proceso.getCodigo())
                .nombres(postulante.getNombresPostulante())
                .apellidos(postulante.getApellidosPostulante())
                .cedula(postulante.getIdentificacion())
                .materia(proceso.getSolicitudDocente().getMateria().getNombreMateria())
                .faseActual(proceso.getFaseActual())
                .progreso(proceso.getProgreso())
                .estadoGeneral(proceso.getEstadoGeneral())
                .fases(fasesDTO)
                .historial(historialDTO)
                .build();
    }

    /**
     * Inicia un proceso de evaluación para un postulante aprobado en prepostulación.
     * Crea las FaseProceso para cada FaseEvaluacion activa, la primera en estado "pendiente"
     * y el resto en "bloqueada".
     */
    public PostulanteDetalleDTO iniciarProceso(Postulante postulante, SolicitudDocente solicitud) {
        if (procesoRepository.existsByPostulante_IdPostulanteAndSolicitudDocente_IdSolicitud(
                postulante.getIdPostulante(), solicitud.getIdSolicitud())) {
            throw new RuntimeException("El postulante ya tiene un proceso activo para esta solicitud");
        }

        List<FaseEvaluacion> fases = faseRepository.findByEstadoTrueOrderByOrdenAsc();
        if (fases.isEmpty())
            throw new RuntimeException("No hay fases de evaluación configuradas");

        // Generar código secuencial: #P001
        long total = procesoRepository.count() + 1;
        String codigo = String.format("#P%03d", total);

        ProcesoEvaluacion proceso = new ProcesoEvaluacion();
        proceso.setPostulante(postulante);
        proceso.setSolicitudDocente(solicitud);
        proceso.setCodigo(codigo);
        proceso.setEstadoGeneral("pendiente");
        proceso.setFaseActual(fases.get(0).getNombre());
        proceso.setProgreso(0);
        proceso.setFechaInicio(LocalDateTime.now());

        proceso = procesoRepository.save(proceso);

        // Crear FaseProceso para cada fase
        for (int i = 0; i < fases.size(); i++) {
            FaseProceso fp = new FaseProceso();
            fp.setProceso(proceso);
            fp.setFase(fases.get(i));
            fp.setEstado(i == 0 ? "pendiente" : "bloqueada");
            faseProcesoRepository.save(fp);
        }

        // Registrar en historial
        registrarHistorial(proceso, "Proceso Iniciado",
                "Postulante ingresado al sistema de evaluación", "Sistema");

        return obtenerDetalle(proceso.getIdProceso());
    }

    /**
     * Avanza al postulante a la siguiente fase (se llama tras completar una evaluación).
     */
    public void avanzarFase(Long idProceso) {
        ProcesoEvaluacion proceso = findOrThrow(idProceso);

        List<FaseProceso> fases = faseProcesoRepository
                .findByProceso_IdProcesoOrderByFase_OrdenAsc(idProceso);

        // Encontrar la fase actual "en_curso" o "pendiente"
        FaseProceso faseActual = fases.stream()
                .filter(f -> "en_curso".equals(f.getEstado()) || "pendiente".equals(f.getEstado()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay fase activa para avanzar"));

        // Marcarla completada
        faseActual.setEstado("completada");
        faseActual.setFechaCompletada(LocalDateTime.now());
        faseProcesoRepository.save(faseActual);

        // Desbloquear la siguiente
        fases.stream()
                .filter(f -> f.getFase().getOrden().equals(faseActual.getFase().getOrden() + 1)
                        && "bloqueada".equals(f.getEstado()))
                .findFirst()
                .ifPresentOrElse(sig -> {
                    sig.setEstado("pendiente");
                    faseProcesoRepository.save(sig);
                    proceso.setFaseActual(sig.getFase().getNombre());
                    proceso.setEstadoGeneral("en_proceso");
                }, () -> {
                    // No hay siguiente → proceso completado
                    proceso.setEstadoGeneral("completado");
                    proceso.setFaseActual("Completado");
                });

        // Recalcular progreso
        long completadas = fases.stream().filter(f -> "completada".equals(f.getEstado())).count();
        proceso.setProgreso((int) Math.round((double) completadas / fases.size() * 100));
        procesoRepository.save(proceso);
    }

    /**
     * Registra la decisión final del comité sobre el postulante.
     */
    public void registrarDecision(Long idProceso, DecisionRequestDTO dto) {
        ProcesoEvaluacion proceso = findOrThrow(idProceso);
        proceso.setDecision(dto.getDecision());
        proceso.setJustificacionDecision(dto.getJustificacion());
        proceso.setFechaDecision(LocalDateTime.now());
        proceso.setEstadoGeneral("completado");
        procesoRepository.save(proceso);

        registrarHistorial(proceso, "Decisión Final Registrada",
                "Decisión: " + dto.getDecision() + " - " + dto.getJustificacion(),
                "Comité");
    }

    // ─── Helpers ───────────────────────────────────────────────

    private PostulanteEvaluacionDTO toListDTO(ProcesoEvaluacion p) {
        Postulante postulante = p.getPostulante();
        return new PostulanteEvaluacionDTO(
                p.getIdProceso(),
                p.getCodigo(),
                postulante.getNombresPostulante(),
                postulante.getApellidosPostulante(),
                postulante.getIdentificacion(),
                p.getSolicitudDocente().getMateria().getNombreMateria(),
                p.getFaseActual(),
                p.getProgreso(),
                p.getEstadoGeneral()
        );
    }

    private PostulanteDetalleDTO.ReunionResumenDTO toReunionResumen(Reunion r, Long idProceso) {
        return PostulanteDetalleDTO.ReunionResumenDTO.builder()
                .idReunion(r.getIdReunion())
                .idPostulante(idProceso)
                .idFase(r.getFaseProceso().getFase().getIdFase())
                .fecha(r.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .hora(r.getHora().toString())
                .duracion(r.getDuracion())
                .modalidad(r.getModalidad())
                .enlace(r.getEnlace())
                .evaluadores(stringALista(r.getEvaluadoresNombres()))
                .observaciones(r.getObservaciones())
                .estado(r.getEstado())
                .build();
    }

    public void registrarHistorial(ProcesoEvaluacion proceso, String titulo,
                                   String descripcion, String usuario) {
        HistorialAccion h = new HistorialAccion();
        h.setProceso(proceso);
        h.setTitulo(titulo);
        h.setDescripcion(descripcion);
        h.setUsuario(usuario);
        h.setFecha(LocalDateTime.now());
        historialRepository.save(h);
    }

    public ProcesoEvaluacion findOrThrow(Long id) {
        return procesoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proceso de evaluación no encontrado con id: " + id));
    }

    private List<String> stringALista(String valor) {
        if (valor == null || valor.isBlank()) return Collections.emptyList();
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
