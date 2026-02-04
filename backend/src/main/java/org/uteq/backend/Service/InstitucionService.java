package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.institucion.InstitucionRequestDTO;
import org.uteq.backend.dto.institucion.InstitucionResponseDTO;

public interface InstitucionService {

    InstitucionResponseDTO crear(InstitucionRequestDTO dto);

    List<InstitucionResponseDTO> listar();

    InstitucionResponseDTO obtenerPorId(Long id);

    InstitucionResponseDTO actualizar(Long id, InstitucionRequestDTO dto);

    void eliminar(Long id);
}
