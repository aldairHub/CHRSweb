package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.FacultadRequestDTO;
import org.uteq.backend.dto.FacultadResponseDTO;

public interface FacultadService {

    FacultadResponseDTO crear(FacultadRequestDTO dto);

    List<FacultadResponseDTO> listar();

    FacultadResponseDTO obtenerPorId(Long id);

    FacultadResponseDTO actualizar(Long id, FacultadRequestDTO dto);

    void eliminar(Long id);
}
