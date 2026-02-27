package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.FaseEvaluacionRequestDTO;
import org.uteq.backend.dto.FaseEvaluacionResponseDTO;
import org.uteq.backend.entity.FaseEvaluacion;
import org.uteq.backend.entity.PlantillaEvaluacion;
import org.uteq.backend.repository.FaseEvaluacionRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FaseEvaluacionService {

    private final FaseEvaluacionRepository faseRepository;

    // ─── Listar ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<FaseEvaluacionResponseDTO> listar() {
        return faseRepository.findAllByOrderByOrdenAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─── Obtener por ID ────────────────────────────────────────
    @Transactional(readOnly = true)
    public FaseEvaluacionResponseDTO obtenerPorId(Long id) {
        FaseEvaluacion fase = faseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada con id: " + id));
        return toDTO(fase);
    }

    // ─── Crear ─────────────────────────────────────────────────
    public FaseEvaluacionResponseDTO crear(FaseEvaluacionRequestDTO dto) {
        FaseEvaluacion fase = new FaseEvaluacion();
        fase.setNombre(dto.getNombre());
        fase.setTipo(dto.getTipo());
        fase.setPeso(dto.getPeso());
        fase.setOrden(dto.getOrden() != null ? dto.getOrden()
                : faseRepository.findAllByOrderByOrdenAsc().size() + 1);
        fase.setEvaluadoresPermitidos(listaAString(dto.getEvaluadoresPermitidos()));
        fase.setEstado(dto.getEstado() != null ? dto.getEstado() : true);
        return toDTO(faseRepository.save(fase));
    }

    // ─── Actualizar ────────────────────────────────────────────
    public FaseEvaluacionResponseDTO actualizar(Long id, FaseEvaluacionRequestDTO dto) {
        FaseEvaluacion fase = faseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fase no encontrada con id: " + id));
        if (dto.getNombre()  != null) fase.setNombre(dto.getNombre());
        if (dto.getTipo()    != null) fase.setTipo(dto.getTipo());
        if (dto.getPeso()    != null) fase.setPeso(dto.getPeso());
        if (dto.getOrden()   != null) fase.setOrden(dto.getOrden());
        if (dto.getEstado()  != null) fase.setEstado(dto.getEstado());
        if (dto.getEvaluadoresPermitidos() != null)
            fase.setEvaluadoresPermitidos(listaAString(dto.getEvaluadoresPermitidos()));
        return toDTO(faseRepository.save(fase));
    }

    // ─── Eliminar ──────────────────────────────────────────────
    public void eliminar(Long id) {
        if (!faseRepository.existsById(id))
            throw new RuntimeException("Fase no encontrada con id: " + id);
        faseRepository.deleteById(id);
    }

    // ─── Mapper ────────────────────────────────────────────────
    public FaseEvaluacionResponseDTO toDTO(FaseEvaluacion f) {
        FaseEvaluacionResponseDTO dto = new FaseEvaluacionResponseDTO();
        dto.setIdFase(f.getIdFase());
        dto.setNombre(f.getNombre());
        dto.setTipo(f.getTipo());
        dto.setPeso(f.getPeso());
        dto.setOrden(f.getOrden());
        dto.setEstado(f.getEstado());
        dto.setEvaluadoresPermitidos(stringALista(f.getEvaluadoresPermitidos()));

        // Si tiene plantilla asociada, exponer id y nombre
        PlantillaEvaluacion plantilla = f.getPlantilla();
        if (plantilla != null) {
            dto.setIdPlantilla(plantilla.getIdPlantilla());
            dto.setNombrePlantilla(plantilla.getNombre());
        }
        return dto;
    }

    // ─── Helpers ───────────────────────────────────────────────
    private String listaAString(List<String> lista) {
        if (lista == null || lista.isEmpty()) return "";
        return String.join(",", lista);
    }

    private List<String> stringALista(String valor) {
        if (valor == null || valor.isBlank()) return Collections.emptyList();
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
