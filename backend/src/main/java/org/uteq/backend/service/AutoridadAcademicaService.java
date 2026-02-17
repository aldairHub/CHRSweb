package org.uteq.backend.service;

import java.util.List;
import org.uteq.backend.dto.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.AutoridadAcademicaResponseDTO;
import org.uteq.backend.dto.AutoridadRegistroRequestDTO;
import org.uteq.backend.dto.AutoridadRegistroResponseDTO;

public interface AutoridadAcademicaService {

    AutoridadAcademicaResponseDTO crear(AutoridadAcademicaRequestDTO dto);

    List<AutoridadAcademicaResponseDTO> listar();

    AutoridadAcademicaResponseDTO obtenerPorId(Long id);

    AutoridadAcademicaResponseDTO actualizar(Long id, AutoridadAcademicaRequestDTO dto);

    void eliminar(Long id);

    AutoridadRegistroResponseDTO registrarAutoridad(AutoridadRegistroRequestDTO dto);

    void cambiarEstado(Long idAutoridad, Boolean estado);
    Long obtenerIdAutoridadPorUsuarioApp(String usuarioApp);
}
