package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.facultad.FacultadRequestDTO;
import org.uteq.backend.dto.facultad.FacultadResponseDTO;

public interface FacultadService {

    FacultadResponseDTO crear(FacultadRequestDTO dto);

    List<FacultadResponseDTO> listar();

    FacultadResponseDTO obtenerPorId(Long id);

    FacultadResponseDTO actualizar(Long id, FacultadRequestDTO dto);

    void eliminar(Long id);
}
