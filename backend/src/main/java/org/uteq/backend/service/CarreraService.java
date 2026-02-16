package org.uteq.backend.service;

import java.util.List;

import org.uteq.backend.dto.CarreraRequestDTO;
import org.uteq.backend.dto.CarreraResponseDTO;

public interface CarreraService {

    CarreraResponseDTO crear(CarreraRequestDTO dto);

    List<CarreraResponseDTO> listar();

    CarreraResponseDTO obtenerPorId(Long id);

    CarreraResponseDTO actualizar(Long id, CarreraRequestDTO dto);

    void eliminar(Long id);
}
