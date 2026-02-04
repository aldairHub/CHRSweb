package org.uteq.backend.Service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.dto.AreaConocimientoRequestDTO;
import org.uteq.backend.dto.AreaConocimientoResponseDTO;
import org.uteq.backend.Entity.AreaConocimiento;
import org.uteq.backend.Repository.AreaConocimientoRepository;
import org.uteq.backend.Service.AreaConocimientoService;

@Service
@Transactional
public class AreaConocimientoServiceImpl implements AreaConocimientoService {

    private final AreaConocimientoRepository repository;

    public AreaConocimientoServiceImpl(AreaConocimientoRepository repository) {
        this.repository = repository;
    }

    @Override
    public AreaConocimientoResponseDTO crear(AreaConocimientoRequestDTO dto) {
        AreaConocimiento area = new AreaConocimiento();
        area.setNombreArea(dto.getNombreArea());

        AreaConocimiento guardado = repository.save(area);
        return mapToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaConocimientoResponseDTO> listar() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AreaConocimientoResponseDTO obtenerPorId(Long id) {
        AreaConocimiento area = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Área de conocimiento no encontrada con id: " + id));

        return mapToResponse(area);
    }

    @Override
    public AreaConocimientoResponseDTO actualizar(Long id, AreaConocimientoRequestDTO dto) {
        AreaConocimiento area = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Área de conocimiento no encontrada con id: " + id));

        area.setNombreArea(dto.getNombreArea());

        AreaConocimiento actualizado = repository.save(area);
        return mapToResponse(actualizado);
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException(
                    "Área de conocimiento no encontrada con id: " + id);
        }
        repository.deleteById(id);
    }

    // =========================
    // Mapper Entity -> DTO
    // =========================
    private AreaConocimientoResponseDTO mapToResponse(AreaConocimiento area) {
        return new AreaConocimientoResponseDTO(
                area.getIdArea(),
                area.getNombreArea()
        );
    }
}
