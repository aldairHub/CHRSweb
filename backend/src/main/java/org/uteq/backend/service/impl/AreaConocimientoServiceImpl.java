package org.uteq.backend.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.dto.AreaConocimientoRequestDTO;
import org.uteq.backend.dto.AreaConocimientoResponseDTO;
import org.uteq.backend.entity.AreaConocimiento;
import org.uteq.backend.repository.AreaConocimientoRepository;
import org.uteq.backend.service.AreaConocimientoService;
import org.uteq.backend.service.NotificacionService;

@Service
@Transactional
public class AreaConocimientoServiceImpl implements AreaConocimientoService {

    private final AreaConocimientoRepository repository;
    private final NotificacionService notifService;

    public AreaConocimientoServiceImpl(AreaConocimientoRepository repository, NotificacionService notifService) {
        this.repository = repository;
        this.notifService = notifService;
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
