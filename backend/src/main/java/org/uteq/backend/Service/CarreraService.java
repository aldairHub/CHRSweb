package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.carrera.CarreraRequestDTO;
import org.uteq.backend.dto.carrera.CarreraResponseDTO;

public interface CarreraService {

    CarreraResponseDTO crear(CarreraRequestDTO dto);

    List<CarreraResponseDTO> listar();

    CarreraResponseDTO obtenerPorId(Long id);

    CarreraResponseDTO actualizar(Long id, CarreraRequestDTO dto);

    void eliminar(Long id);
}
