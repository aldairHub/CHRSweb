package org.uteq.backend.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.entity.Institucion;
import org.uteq.backend.repository.InstitucionRepository;
import org.uteq.backend.service.InstitucionService;
import org.uteq.backend.dto.InstitucionRequestDTO;
import org.uteq.backend.dto.InstitucionResponseDTO;

@Service
@Transactional
public class InstitucionServiceImpl implements InstitucionService {

    private final InstitucionRepository institucionRepository;

    public InstitucionServiceImpl(InstitucionRepository institucionRepository) {
        this.institucionRepository = institucionRepository;
    }

    @Override
    public InstitucionResponseDTO crear(InstitucionRequestDTO dto) {

        Institucion institucion = new Institucion();
        institucion.setNombre(dto.getNombreInstitucion());
        institucion.setDireccion(dto.getDireccion());
        institucion.setTelefono(dto.getTelefono());


        return mapToResponse(institucionRepository.save(institucion));
    }

    @Override
    public List<InstitucionResponseDTO> listar() {
        return institucionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InstitucionResponseDTO obtenerPorId(Long id) {
        Institucion institucion = institucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Institucion no encontrada"));
        return mapToResponse(institucion);
    }

    @Override
    public InstitucionResponseDTO actualizar(Long id, InstitucionRequestDTO dto) {

        Institucion institucion = institucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Institucion no encontrada"));

        institucion.setNombre(dto.getNombreInstitucion());
        institucion.setDireccion(dto.getDireccion());
        institucion.setTelefono(dto.getTelefono());


        return mapToResponse(institucionRepository.save(institucion));
    }

    @Override
    public void eliminar(Long id) {
        institucionRepository.deleteById(id);
    }

    private InstitucionResponseDTO mapToResponse(Institucion institucion) {
        InstitucionResponseDTO dto = new InstitucionResponseDTO();
        dto.setIdInstitucion(institucion.getIdInstitucion());
        dto.setNombreInstitucion(institucion.getNombre());
        dto.setDireccion(institucion.getDireccion());
        dto.setTelefono(institucion.getTelefono());
        return dto;
    }
}
