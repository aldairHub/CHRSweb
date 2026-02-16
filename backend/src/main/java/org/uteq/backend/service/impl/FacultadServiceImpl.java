package org.uteq.backend.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.entity.Facultad;
import org.uteq.backend.repository.FacultadRepository;
import org.uteq.backend.service.FacultadService;
import org.uteq.backend.dto.FacultadRequestDTO;
import org.uteq.backend.dto.FacultadResponseDTO;

@Service
@Transactional
public class FacultadServiceImpl implements FacultadService {

    private final FacultadRepository facultadRepository;

    public FacultadServiceImpl(FacultadRepository facultadRepository) {
        this.facultadRepository = facultadRepository;
    }

    @Override
    public FacultadResponseDTO crear(FacultadRequestDTO dto) {

        String nombre = dto.getNombreFacultad() == null ? "" : dto.getNombreFacultad().trim();
        if (nombre.isEmpty()) {
            throw new RuntimeException("El nombre de la facultad es obligatorio");
        }

        Facultad facultad = new Facultad();
        facultad.setNombreFacultad(nombre);
        facultad.setEstado(dto.getEstado() != null ? dto.getEstado() : true);

        Facultad guardada = facultadRepository.save(facultad);
        return mapToResponse(guardada);
    }

    @Override
    public FacultadResponseDTO actualizar(Long id, FacultadRequestDTO dto) {

        Facultad facultad = facultadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));

        String nombre = dto.getNombreFacultad() == null ? "" : dto.getNombreFacultad().trim();
        if (nombre.isEmpty()) {
            throw new RuntimeException("El nombre de la facultad es obligatorio");
        }

        facultad.setNombreFacultad(nombre);
        if (dto.getEstado() != null) {
            facultad.setEstado(dto.getEstado());
        }

        return mapToResponse(facultadRepository.save(facultad));
    }

    @Override
    public List<FacultadResponseDTO> listar() {
        return facultadRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FacultadResponseDTO obtenerPorId(Long id) {
        Facultad facultad = facultadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));
        return mapToResponse(facultad);
    }


    @Override
    public void eliminar(Long id) {
        facultadRepository.deleteById(id);
    }

    private FacultadResponseDTO mapToResponse(Facultad facultad) {
        FacultadResponseDTO dto = new FacultadResponseDTO();
        dto.setIdFacultad(facultad.getIdFacultad());
        dto.setNombreFacultad(facultad.getNombreFacultad());
        dto.setEstado(facultad.isEstado());
        return dto;
    }
}
