package org.uteq.backend.Service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.Entity.Carrera;
import org.uteq.backend.Entity.Facultad;
import org.uteq.backend.Repository.CarreraRepository;
import org.uteq.backend.Repository.FacultadRepository;
import org.uteq.backend.Service.CarreraService;
import org.uteq.backend.dto.carrera.CarreraRequestDTO;
import org.uteq.backend.dto.carrera.CarreraResponseDTO;

@Service
@Transactional
public class CarreraServiceImpl implements CarreraService {

    private final CarreraRepository carreraRepository;
    private final FacultadRepository facultadRepository;

    public CarreraServiceImpl(
            CarreraRepository carreraRepository,
            FacultadRepository facultadRepository) {
        this.carreraRepository = carreraRepository;
        this.facultadRepository = facultadRepository;
    }

    @Override
    public CarreraResponseDTO crear(CarreraRequestDTO dto) {

        Facultad facultad = facultadRepository.findById(dto.getIdFacultad())
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));

        Carrera carrera = new Carrera();
        carrera.setFacultad(facultad);
        carrera.setNombreCarrera(dto.getNombreCarrera());
        carrera.setModalidad(dto.getModalidad());
        carrera.setEstado(dto.isEstado());

        Carrera guardada = carreraRepository.save(carrera);
        return mapToResponse(guardada);
    }

    @Override
    public List<CarreraResponseDTO> listar() {
        return carreraRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CarreraResponseDTO obtenerPorId(Long id) {
        Carrera carrera = carreraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));
        return mapToResponse(carrera);
    }

    @Override
    public CarreraResponseDTO actualizar(Long id, CarreraRequestDTO dto) {

        Carrera carrera = carreraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));

        Facultad facultad = facultadRepository.findById(dto.getIdFacultad())
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));

        carrera.setFacultad(facultad);
        carrera.setNombreCarrera(dto.getNombreCarrera());
        carrera.setModalidad(dto.getModalidad());
        carrera.setEstado(dto.isEstado());

        return mapToResponse(carreraRepository.save(carrera));
    }

    @Override
    public void eliminar(Long id) {
        carreraRepository.deleteById(id);
    }

    private CarreraResponseDTO mapToResponse(Carrera carrera) {
        CarreraResponseDTO dto = new CarreraResponseDTO();
        dto.setIdCarrera(carrera.getIdCarrera());
        dto.setIdFacultad(carrera.getFacultad().getIdFacultad());
        dto.setNombreCarrera(carrera.getNombreCarrera());
        dto.setModalidad(carrera.getModalidad());
        dto.setEstado(carrera.isEstado());
        return dto;
    }
}
