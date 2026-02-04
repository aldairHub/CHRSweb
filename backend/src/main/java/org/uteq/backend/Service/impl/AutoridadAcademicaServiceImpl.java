package org.uteq.backend.Service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.Entity.AutoridadAcademica;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Entity.Institucion;

import org.uteq.backend.Repository.AutoridadAcademicaRepository;
import org.uteq.backend.Repository.UsuarioRepository;
import org.uteq.backend.Repository.InstitucionRepository;

import org.uteq.backend.Service.AutoridadAcademicaService;
import org.uteq.backend.dto.autoridad.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.autoridad.AutoridadAcademicaResponseDTO;

@Service
@Transactional
public class AutoridadAcademicaServiceImpl implements AutoridadAcademicaService {

    private final AutoridadAcademicaRepository autoridadRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstitucionRepository institucionRepository;

    public AutoridadAcademicaServiceImpl(
            AutoridadAcademicaRepository autoridadRepository,
            UsuarioRepository usuarioRepository,
            InstitucionRepository institucionRepository) {

        this.autoridadRepository = autoridadRepository;
        this.usuarioRepository = usuarioRepository;
        this.institucionRepository = institucionRepository;
    }

    @Override
    public AutoridadAcademicaResponseDTO crear(AutoridadAcademicaRequestDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Institucion institucion = institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institucion no encontrada"));

        AutoridadAcademica autoridad = new AutoridadAcademica();
        autoridad.setNombres(dto.getNombres());
        autoridad.setApellidos(dto.getApellidos());
        autoridad.setCorreo(dto.getCorreo());
        autoridad.setFechaNacimiento(dto.getFechaNacimiento());
        autoridad.setEstado(dto.isEstado());
        autoridad.setUsuario(usuario);
        autoridad.setInstitucion(institucion);

        return mapToResponse(autoridadRepository.save(autoridad));
    }

    @Override
    public List<AutoridadAcademicaResponseDTO> listar() {
        return autoridadRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AutoridadAcademicaResponseDTO obtenerPorId(Long id) {
        AutoridadAcademica autoridad = autoridadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Autoridad no encontrada"));
        return mapToResponse(autoridad);
    }

    @Override
    public AutoridadAcademicaResponseDTO actualizar(Long id, AutoridadAcademicaRequestDTO dto) {

        AutoridadAcademica autoridad = autoridadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Autoridad no encontrada"));

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Institucion institucion = institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institucion no encontrada"));

        autoridad.setNombres(dto.getNombres());
        autoridad.setApellidos(dto.getApellidos());
        autoridad.setCorreo(dto.getCorreo());
        autoridad.setFechaNacimiento(dto.getFechaNacimiento());
        autoridad.setEstado(dto.isEstado());
        autoridad.setUsuario(usuario);
        autoridad.setInstitucion(institucion);

        return mapToResponse(autoridadRepository.save(autoridad));
    }

    @Override
    public void eliminar(Long id) {
        autoridadRepository.deleteById(id);
    }

    private AutoridadAcademicaResponseDTO mapToResponse(AutoridadAcademica autoridad) {
        AutoridadAcademicaResponseDTO dto = new AutoridadAcademicaResponseDTO();
        dto.setIdAutoridad(autoridad.getIdAutoridad());
        dto.setNombres(autoridad.getNombres());
        dto.setApellidos(autoridad.getApellidos());
        dto.setCorreo(autoridad.getCorreo());
        dto.setFechaNacimiento(autoridad.getFechaNacimiento());
        dto.setEstado(autoridad.isEstado());
        dto.setIdUsuario(autoridad.getUsuario().getIdUsuario());
        dto.setIdInstitucion(autoridad.getInstitucion().getIdInstitucion());
        return dto;
    }
}
