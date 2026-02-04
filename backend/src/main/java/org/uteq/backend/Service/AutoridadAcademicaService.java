package org.uteq.backend.Service;

import java.util.List;
import org.uteq.backend.dto.autoridad.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.autoridad.AutoridadAcademicaResponseDTO;

public interface AutoridadAcademicaService {

    AutoridadAcademicaResponseDTO crear(AutoridadAcademicaRequestDTO dto);

    List<AutoridadAcademicaResponseDTO> listar();

    AutoridadAcademicaResponseDTO obtenerPorId(Long id);

    AutoridadAcademicaResponseDTO actualizar(Long id, AutoridadAcademicaRequestDTO dto);

    void eliminar(Long id);
}
