package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.PlantillaEvaluacionRequestDTO;
import org.uteq.backend.dto.PlantillaEvaluacionResponseDTO;
import org.uteq.backend.entity.FaseEvaluacion;
import org.uteq.backend.entity.PlantillaEvaluacion;
import org.uteq.backend.repository.FaseEvaluacionRepository;
import org.uteq.backend.repository.PlantillaEvaluacionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlantillaEvaluacionService {

    private final PlantillaEvaluacionRepository plantillaRepository;
    private final FaseEvaluacionRepository faseRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── Listar ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PlantillaEvaluacionResponseDTO> listar() {
        return plantillaRepository.findAllByOrderByUltimaModificacionDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─── Obtener por ID ────────────────────────────────────────
    @Transactional(readOnly = true)
    public PlantillaEvaluacionResponseDTO obtenerPorId(Long id) {
        return toDTO(findOrThrow(id));
    }

    // ─── Crear ─────────────────────────────────────────────────
    public PlantillaEvaluacionResponseDTO crear(PlantillaEvaluacionRequestDTO dto) {
        if (plantillaRepository.existsByCodigo(dto.getCodigo()))
            throw new RuntimeException("Ya existe una plantilla con el código: " + dto.getCodigo());

        FaseEvaluacion fase = faseRepository.findById(dto.getIdFase())
                .orElseThrow(() -> new RuntimeException("Fase no encontrada con id: " + dto.getIdFase()));

        PlantillaEvaluacion p = new PlantillaEvaluacion();
        p.setCodigo(dto.getCodigo());
        p.setNombre(dto.getNombre());
        p.setFase(fase);
        p.setEstado(dto.getEstado() != null ? dto.getEstado() : true);
        p.setUltimaModificacion(LocalDateTime.now());

        return toDTO(plantillaRepository.save(p));
    }

    // ─── Actualizar ────────────────────────────────────────────
    public PlantillaEvaluacionResponseDTO actualizar(Long id, PlantillaEvaluacionRequestDTO dto) {
        PlantillaEvaluacion p = findOrThrow(id);

        if (dto.getCodigo() != null) {
            if (!dto.getCodigo().equals(p.getCodigo()) && plantillaRepository.existsByCodigo(dto.getCodigo()))
                throw new RuntimeException("Ya existe una plantilla con el código: " + dto.getCodigo());
            p.setCodigo(dto.getCodigo());
        }
        if (dto.getNombre()  != null) p.setNombre(dto.getNombre());
        if (dto.getEstado()  != null) p.setEstado(dto.getEstado());
        if (dto.getIdFase()  != null) {
            FaseEvaluacion fase = faseRepository.findById(dto.getIdFase())
                    .orElseThrow(() -> new RuntimeException("Fase no encontrada con id: " + dto.getIdFase()));
            p.setFase(fase);
        }
        p.setUltimaModificacion(LocalDateTime.now());

        return toDTO(plantillaRepository.save(p));
    }

    // ─── Eliminar ──────────────────────────────────────────────
    public void eliminar(Long id) {
        if (!plantillaRepository.existsById(id))
            throw new RuntimeException("Plantilla no encontrada con id: " + id);
        plantillaRepository.deleteById(id);
    }

    // ─── Mapper ────────────────────────────────────────────────
    private PlantillaEvaluacionResponseDTO toDTO(PlantillaEvaluacion p) {
        PlantillaEvaluacionResponseDTO dto = new PlantillaEvaluacionResponseDTO();
        dto.setIdPlantilla(p.getIdPlantilla());
        dto.setCodigo(p.getCodigo());
        dto.setNombre(p.getNombre());
        dto.setIdFase(p.getFase().getIdFase());
        dto.setNombreFase(p.getFase().getNombre());
        dto.setNumeroCriterios(p.getCriterios() != null ? p.getCriterios().size() : 0);
        dto.setUltimaModificacion(p.getUltimaModificacion() != null
                ? p.getUltimaModificacion().format(FMT) : "");
        dto.setEstado(p.getEstado());
        return dto;
    }

    private PlantillaEvaluacion findOrThrow(Long id) {
        return plantillaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con id: " + id));
    }
}
