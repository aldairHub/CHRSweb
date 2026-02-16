package org.uteq.backend.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.uteq.backend.entity.Carrera;
import org.uteq.backend.entity.Materia;
import org.uteq.backend.repository.CarreraRepository;
import org.uteq.backend.repository.MateriaRepository;
import org.uteq.backend.service.MateriaService;
import org.uteq.backend.dto.MateriaRequestDTO;
import org.uteq.backend.dto.MateriaResponseDTO;

@Service
public class MateriaServiceImpl implements MateriaService {

    private final MateriaRepository materiaRepository;
    private final CarreraRepository carreraRepository;

    public MateriaServiceImpl(
            MateriaRepository materiaRepository,
            CarreraRepository carreraRepository) {
        this.materiaRepository = materiaRepository;
        this.carreraRepository = carreraRepository;
    }

    @Override
    public MateriaResponseDTO crear(MateriaRequestDTO dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty())
            throw new RuntimeException("Nombre obligatorio");

        if (dto.getIdCarrera() == null)
            throw new RuntimeException("Carrera obligatoria");

        if (dto.getNivel() == null)
            throw new RuntimeException("Nivel obligatorio");

        Carrera carrera = carreraRepository.findById(dto.getIdCarrera())
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));

        Materia materia = new Materia();
        materia.setNombreMateria(dto.getNombre());
        materia.setCarrera(carrera);
        materia.setNivel(dto.getNivel());

        materia = materiaRepository.save(materia);

        return mapToDTO(materia);
    }
    @Override
    public MateriaResponseDTO actualizar(Long id, MateriaRequestDTO dto) {

        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Materia no encontrada"));

        Carrera carrera = carreraRepository.findById(dto.getIdCarrera())
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));

        materia.setNombreMateria(dto.getNombre());
        materia.setNivel(dto.getNivel());
        materia.setCarrera(carrera);

        materia = materiaRepository.save(materia);

        return mapToDTO(materia);
    }


    @Override
    public List<MateriaResponseDTO> listar() {
        return materiaRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MateriaResponseDTO> listarPorCarrera(Long idCarrera) {
        return materiaRepository.findByCarreraIdCarrera(idCarrera)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private MateriaResponseDTO mapToDTO(Materia materia) {
        MateriaResponseDTO dto = new MateriaResponseDTO();
        dto.setIdMateria(materia.getIdMateria());
        dto.setNombre(materia.getNombreMateria());
        dto.setIdCarrera(materia.getCarrera().getIdCarrera());
        dto.setNombreCarrera(materia.getCarrera().getNombreCarrera());
        dto.setNivel(materia.getNivel());
        return dto;
    }
}
