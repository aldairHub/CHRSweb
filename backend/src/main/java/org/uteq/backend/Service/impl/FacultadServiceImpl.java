package org.uteq.backend.Service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.Entity.Facultad;
import org.uteq.backend.Repository.FacultadRepository;
import org.uteq.backend.Service.FacultadService;
import org.uteq.backend.dto.facultad.FacultadRequestDTO;
import org.uteq.backend.dto.facultad.FacultadResponseDTO;

@Service
@Transactional
public class FacultadServiceImpl implements FacultadService {

    private final FacultadRepository facultadRepository;

    public FacultadServiceImpl(FacultadRepository facultadRepository) {
        this.facultadRepository = facultadRepository;
    }

    @Override
    public FacultadResponseDTO crear(FacultadRequestDTO dto) {

        Facultad facultad = new Facultad();
        facultad.setNombreFacultad(dto.getNombreFacultad());
        facultad.setEstado(dto.isEstado());

        Facultad guardada = facultadRepository.save(facultad);
        return mapToResponse(guardada);
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
    public FacultadResponseDTO actualizar(Long id, FacultadRequestDTO dto) {

        Facultad facultad = facultadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));

        facultad.setNombreFacultad(dto.getNombreFacultad());
        facultad.setEstado(dto.isEstado());

        return mapToResponse(facultadRepository.save(facultad));
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
