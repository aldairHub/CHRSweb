package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.ReunionRequestDTO;
import org.uteq.backend.dto.ReunionResponseDTO;
import org.uteq.backend.entity.*;
import org.uteq.backend.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReunionService {

    private final ReunionRepository reunionRepository;
    private final FaseProcesoRepository faseProcesoRepository;
    private final ProcesoEvaluacionRepository procesoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcesoEvaluacionService procesoService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─── Programar reunión ─────────────────────────────────────
    public ReunionResponseDTO programar(ReunionRequestDTO dto) {

        // idPostulante del frontend = idProceso en backend
        ProcesoEvaluacion proceso = procesoService.findOrThrow(dto.getIdPostulante());

        FaseProceso faseProceso = faseProcesoRepository
                .findByProceso_IdProcesoAndFase_IdFase(proceso.getIdProceso(), dto.getIdFase())
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró la fase " + dto.getIdFase() + " en este proceso"));

        // Resolver nombres de evaluadores
        List<String> nombresEvaluadores = dto.getEvaluadoresIds() != null
                ? dto.getEvaluadoresIds().stream()
                .map(id -> usuarioRepository.findById(id)
                        .map(Usuario::getUsuarioApp)
                        .orElse("Usuario #" + id))
                .collect(Collectors.toList())
                : List.of();

        String idsStr     = dto.getEvaluadoresIds() != null
                ? dto.getEvaluadoresIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                : "";
        String nombresStr = String.join(",", nombresEvaluadores);

        // Si ya existe reunión para esta fase-proceso, actualizar
        Reunion reunion = reunionRepository
                .findByFaseProceso_IdFaseProceso(faseProceso.getIdFaseProceso())
                .orElseGet(Reunion::new);

        reunion.setFaseProceso(faseProceso);
        reunion.setFecha(LocalDate.parse(dto.getFecha(), DATE_FMT));
        reunion.setHora(LocalTime.parse(dto.getHora()));
        reunion.setDuracion(dto.getDuracion());
        reunion.setModalidad(dto.getModalidad());
        reunion.setEnlace(dto.getEnlace());
        reunion.setEvaluadoresIds(idsStr);
        reunion.setEvaluadoresNombres(nombresStr);
        reunion.setObservaciones(dto.getObservaciones());
        reunion.setEstado("programada");

        reunion = reunionRepository.save(reunion);

        // Actualizar estado de la fase-proceso a "en_curso" si estaba pendiente
        if ("pendiente".equals(faseProceso.getEstado())) {
            faseProceso.setEstado("en_curso");
            faseProceso.setEvaluadoresAsignados(nombresStr);
            faseProcesoRepository.save(faseProceso);
        }

        // Registrar en historial
        procesoService.registrarHistorial(proceso,
                "Reunión Programada – " + faseProceso.getFase().getNombre(),
                "Reunión programada para el " + dto.getFecha() + " a las " + dto.getHora()
                        + " por modalidad " + dto.getModalidad(),
                "Admin Sistema");

        return toDTO(reunion, proceso.getIdProceso());
    }

    // ─── Listar reuniones programadas ──────────────────────────
    @Transactional(readOnly = true)
    public List<ReunionResponseDTO> listarProgramadas() {
        return reunionRepository.findByEstadoOrderByFechaAscHoraAsc("programada")
                .stream()
                .map(r -> toDTO(r, r.getFaseProceso().getProceso().getIdProceso()))
                .collect(Collectors.toList());
    }

    // ─── Obtener por ID ────────────────────────────────────────
    @Transactional(readOnly = true)
    public ReunionResponseDTO obtenerPorId(Long idReunion) {
        Reunion r = reunionRepository.findById(idReunion)
                .orElseThrow(() -> new RuntimeException("Reunión no encontrada con id: " + idReunion));
        return toDTO(r, r.getFaseProceso().getProceso().getIdProceso());
    }

    // ─── Cancelar reunión ──────────────────────────────────────
    public ReunionResponseDTO cancelar(Long idReunion, String motivo) {
        Reunion r = reunionRepository.findById(idReunion)
                .orElseThrow(() -> new RuntimeException("Reunión no encontrada con id: " + idReunion));
        r.setEstado("cancelada");
        r = reunionRepository.save(r);

        ProcesoEvaluacion proceso = r.getFaseProceso().getProceso();
        procesoService.registrarHistorial(proceso, "Reunión Cancelada",
                "Motivo: " + (motivo != null ? motivo : "Sin especificar"), "Admin Sistema");

        return toDTO(r, proceso.getIdProceso());
    }

    // ─── Mapper ────────────────────────────────────────────────
    private ReunionResponseDTO toDTO(Reunion r, Long idProceso) {
        FaseProceso fp  = r.getFaseProceso();
        Postulante   p  = fp.getProceso().getPostulante();

        return ReunionResponseDTO.builder()
                .idReunion(r.getIdReunion())
                .idPostulante(idProceso)
                .idFase(fp.getFase().getIdFase())
                .nombrePostulante(p.getNombresPostulante() + " " + p.getApellidosPostulante())
                .nombreFase(fp.getFase().getNombre())
                .fecha(r.getFecha().format(DATE_FMT))
                .hora(r.getHora().toString())
                .duracion(r.getDuracion())
                .modalidad(r.getModalidad())
                .enlace(r.getEnlace())
                .evaluadores(stringALista(r.getEvaluadoresNombres()))
                .observaciones(r.getObservaciones())
                .estado(r.getEstado())
                .build();
    }

    private java.util.List<String> stringALista(String valor) {
        if (valor == null || valor.isBlank()) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(valor.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }
}
