package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.EvaluacionRequestDTO;
import org.uteq.backend.dto.EvaluacionResponseDTO;
import org.uteq.backend.dto.ResultadoPostulanteDTO;
import org.uteq.backend.entity.*;
import org.uteq.backend.repository.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final DetalleEvaluacionRepository detalleRepository;
    private final CriterioEvaluacionRepository criterioRepository;
    private final ReunionRepository reunionRepository;
    private final ProcesoEvaluacionService procesoService;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── Confirmar evaluación ──────────────────────────────────
    public EvaluacionResponseDTO confirmar(EvaluacionRequestDTO dto, Long idEvaluador) {

        Reunion reunion = reunionRepository.findById(dto.getIdReunion())
                .orElseThrow(() -> new RuntimeException("Reunión no encontrada con id: " + dto.getIdReunion()));

        Usuario evaluador = usuarioRepository.findById(idEvaluador)
                .orElseThrow(() -> new RuntimeException("Evaluador no encontrado con id: " + idEvaluador));

        // Validar que el evaluador no haya evaluado ya esta reunión
        if (evaluacionRepository.existsByReunion_IdReunionAndEvaluador_IdUsuario(dto.getIdReunion(), idEvaluador))
            throw new RuntimeException("Ya registraste una evaluación para esta reunión");

        // Validar declaración de no conflicto
        if (!Boolean.TRUE.equals(dto.getDeclaroSinConflicto()))
            throw new RuntimeException("Debe declarar que no tiene conflicto de interés");

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setReunion(reunion);
        evaluacion.setEvaluador(evaluador);
        evaluacion.setObservaciones(dto.getObservaciones());
        evaluacion.setDeclaroSinConflicto(true);
        evaluacion.setFirmaDigital(dto.getFirmaDigital());
        evaluacion.setFechaEvaluacion(LocalDateTime.now());
        evaluacion.setConfirmada(true);

        // Guardar cabecera primero para obtener ID
        evaluacion = evaluacionRepository.save(evaluacion);

        // Guardar detalles por criterio y calcular calificación final ponderada
        double calificacionFinal = 0.0;
        for (EvaluacionRequestDTO.DetalleRequest d : dto.getCriterios()) {
            CriterioEvaluacion criterio = criterioRepository.findById(d.getIdCriterio())
                    .orElseThrow(() -> new RuntimeException("Criterio no encontrado: " + d.getIdCriterio()));

            DetalleEvaluacion detalle = new DetalleEvaluacion();
            detalle.setEvaluacion(evaluacion);
            detalle.setCriterio(criterio);
            detalle.setNota(d.getNota());
            detalle.setObservacion(d.getObservacion());
            detalleRepository.save(detalle);

            // Contribución ponderada: (nota / escalaMax) * peso
            double escalaMax = obtenerEscalaMax(criterio.getEscala());
            calificacionFinal += (d.getNota() / escalaMax) * criterio.getPeso();
        }

        evaluacion.setCalificacionFinal(Math.round(calificacionFinal * 100.0) / 100.0);
        evaluacion = evaluacionRepository.save(evaluacion);

        // Verificar si TODOS los evaluadores de la reunión ya enviaron su evaluación
        // y si es así, calcular promedio de fase y avanzar
        verificarYCerrarFase(reunion, evaluacion);

        return toDTO(evaluacion);
    }

    // ─── Obtener evaluaciones de una reunión ───────────────────
    @Transactional(readOnly = true)
    public List<EvaluacionResponseDTO> obtenerPorReunion(Long idReunion) {
        return evaluacionRepository.findByReunion_IdReunion(idReunion)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─── Resultados completos de un postulante ─────────────────
    @Transactional(readOnly = true)
    public ResultadoPostulanteDTO obtenerResultados(Long idProceso) {
        ProcesoEvaluacion proceso = procesoService.findOrThrow(idProceso);
        Postulante postulante = proceso.getPostulante();

        List<FaseProceso> fasesProceso = proceso.getFasesProceso();

        List<ResultadoPostulanteDTO.ResultadoFaseDTO> fasesDTO = fasesProceso.stream()
                .sorted((a, b) -> a.getFase().getOrden().compareTo(b.getFase().getOrden()))
                .map(fp -> {
                    // Obtener evaluaciones de la reunión de esta fase
                    List<EvaluacionResponseDTO> evals = reunionRepository
                            .findByFaseProceso_IdFaseProceso(fp.getIdFaseProceso())
                            .map(r -> evaluacionRepository.findByReunion_IdReunion(r.getIdReunion())
                                    .stream().map(this::toDTO).collect(Collectors.toList()))
                            .orElse(List.of());

                    // Estado de la fase para resultados
                    String estadoResultado = mapearEstadoFaseResultado(fp.getEstado());

                    // Ponderado = calificación * peso / 100
                    Double ponderado = fp.getCalificacion() != null
                            ? Math.round(fp.getCalificacion() * fp.getFase().getPeso()) / 100.0
                            : null;

                    return ResultadoPostulanteDTO.ResultadoFaseDTO.builder()
                            .idFase(fp.getFase().getIdFase())
                            .nombreFase(fp.getFase().getOrden() + ". " + fp.getFase().getNombre())
                            .peso(fp.getFase().getPeso())
                            .calificacion(fp.getCalificacion())
                            .ponderado(ponderado)
                            .evaluadores(stringALista(fp.getEvaluadoresAsignados()))
                            .estado(estadoResultado)
                            .evaluaciones(evals)
                            .build();
                })
                .collect(Collectors.toList());

        double total = fasesDTO.stream()
                .mapToDouble(f -> f.getPonderado() != null ? f.getPonderado() : 0.0)
                .sum();

        return ResultadoPostulanteDTO.builder()
                .idPostulante(idProceso)
                .nombreCompleto(postulante.getNombresPostulante() + " " + postulante.getApellidosPostulante())
                .materia(proceso.getSolicitudDocente().getMateria().getNombreMateria())
                .fasesResultados(fasesDTO)
                .calificacionTotal(Math.round(total * 100.0) / 100.0)
                .progreso(proceso.getProgreso())
                .decision(proceso.getDecision())
                .justificacionDecision(proceso.getJustificacionDecision())
                .build();
    }

    // ─── Helpers privados ──────────────────────────────────────

    /**
     * Después de confirmar cada evaluación, verifica si ya están todos los evaluadores.
     * Si sí: calcula promedio, actualiza calificación en FaseProceso y avanza al siguiente.
     */
    private void verificarYCerrarFase(Reunion reunion, Evaluacion nuevaEval) {
        FaseProceso faseProceso = reunion.getFaseProceso();
        ProcesoEvaluacion proceso = faseProceso.getProceso();

        // Contar cuántos evaluadores tiene asignados la reunión
        long totalEvaluadores = contarEvaluadores(reunion.getEvaluadoresIds());
        long evaluacionesCompletas = evaluacionRepository
                .findByReunion_IdReunion(reunion.getIdReunion())
                .stream().filter(Evaluacion::getConfirmada).count();

        if (evaluacionesCompletas >= totalEvaluadores && totalEvaluadores > 0) {
            // Calcular promedio de todos los evaluadores
            double promedio = evaluacionRepository.findByReunion_IdReunion(reunion.getIdReunion())
                    .stream()
                    .filter(Evaluacion::getConfirmada)
                    .mapToDouble(Evaluacion::getCalificacionFinal)
                    .average()
                    .orElse(0.0);

            // Actualizar calificación en FaseProceso
            faseProceso.setCalificacion(Math.round(promedio * 100.0) / 100.0);
            faseProceso.setEstado("completada");
            faseProceso.setFechaCompletada(LocalDateTime.now());

            // Actualizar estado de la reunión
            reunion.setEstado("completada");
            reunionRepository.save(reunion);

            // Registrar historial
            procesoService.registrarHistorial(proceso,
                    "Fase Completada – " + faseProceso.getFase().getNombre(),
                    "Calificación promedio: " + faseProceso.getCalificacion() + "/5.0",
                    nuevaEval.getEvaluador().getUsuarioApp());

            // Avanzar a la siguiente fase
            procesoService.avanzarFase(proceso.getIdProceso());
        }
    }

    private double obtenerEscalaMax(String escala) {
        return switch (escala) {
            case "1-10"  -> 10.0;
            case "0-100" -> 100.0;
            default      -> 5.0;   // "1-5"
        };
    }

    private long contarEvaluadores(String idsStr) {
        if (idsStr == null || idsStr.isBlank()) return 0;
        return idsStr.split(",").length;
    }

    private String mapearEstadoFaseResultado(String estadoFase) {
        return switch (estadoFase) {
            case "completada" -> "completada";
            case "en_curso"   -> "programada";
            case "pendiente"  -> "pendiente";
            default           -> "bloqueada";
        };
    }

    private EvaluacionResponseDTO toDTO(Evaluacion e) {
        List<EvaluacionResponseDTO.CriterioEvaluadoDTO> criterios = e.getDetalles().stream()
                .map(d -> EvaluacionResponseDTO.CriterioEvaluadoDTO.builder()
                        .idCriterio(d.getCriterio().getIdCriterio())
                        .nombre(d.getCriterio().getNombre())
                        .peso(d.getCriterio().getPeso())
                        .nota(d.getNota())
                        .observacion(d.getObservacion())
                        .build())
                .collect(Collectors.toList());

        return EvaluacionResponseDTO.builder()
                .idEvaluacion(e.getIdEvaluacion())
                .idReunion(e.getReunion().getIdReunion())
                .idEvaluador(e.getEvaluador().getIdUsuario())
                .nombreEvaluador(e.getEvaluador().getUsuarioApp())
                .criterios(criterios)
                .observaciones(e.getObservaciones())
                .calificacionFinal(e.getCalificacionFinal())
                .fechaEvaluacion(e.getFechaEvaluacion() != null
                        ? e.getFechaEvaluacion().format(DT_FMT) : "")
                .firmaDigital(e.getFirmaDigital())
                .confirmada(e.getConfirmada())
                .build();
    }

    private List<String> stringALista(String valor) {
        if (valor == null || valor.isBlank()) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(valor.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
