package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.AutoridadAcademicaResponseDTO;

public interface AutoridadAcademicaService {

    AutoridadAcademicaResponseDTO crear(AutoridadAcademicaRequestDTO dto);

    List<AutoridadAcademicaResponseDTO> listar();

    AutoridadAcademicaResponseDTO obtenerPorId(Long id);

    AutoridadAcademicaResponseDTO actualizar(Long id, AutoridadAcademicaRequestDTO dto);

    void eliminar(Long id);
}
