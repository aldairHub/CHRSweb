package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.MateriaRequestDTO;
import org.uteq.backend.dto.MateriaResponseDTO;

public interface MateriaService {

    MateriaResponseDTO crear(MateriaRequestDTO dto);
    MateriaResponseDTO actualizar(Long id, MateriaRequestDTO dto);

    List<MateriaResponseDTO> listar();
    List<MateriaResponseDTO> listarPorCarrera(Long idCarrera);
}
