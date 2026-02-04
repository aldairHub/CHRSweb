package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.AreaConocimientoRequestDTO;
import org.uteq.backend.dto.AreaConocimientoResponseDTO;

public interface AreaConocimientoService {

    AreaConocimientoResponseDTO crear(AreaConocimientoRequestDTO dto);

    List<AreaConocimientoResponseDTO> listar();

    AreaConocimientoResponseDTO obtenerPorId(Long id);

    AreaConocimientoResponseDTO actualizar(Long id, AreaConocimientoRequestDTO dto);

    void eliminar(Long id);
}
