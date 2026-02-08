package org.uteq.backend.Service.impl;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.Entity.AutoridadAcademica;
import org.uteq.backend.Entity.RolAutoridad;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Entity.Institucion;

import org.uteq.backend.Repository.AutoridadAcademicaRepository;
import org.uteq.backend.Repository.IRolAutoridadRepository;
import org.uteq.backend.Repository.UsuarioRepository;
import org.uteq.backend.Repository.InstitucionRepository;

import org.uteq.backend.Service.AutoridadAcademicaService;
import org.uteq.backend.Service.EmailService;
import org.uteq.backend.dto.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.AutoridadAcademicaResponseDTO;
import org.uteq.backend.dto.AutoridadRegistroRequestDTO;
import org.uteq.backend.dto.AutoridadRegistroResponseDTO;

@Service
@Transactional
public class AutoridadAcademicaServiceImpl implements AutoridadAcademicaService {

    private final AutoridadAcademicaRepository autoridadRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstitucionRepository institucionRepository;
    private final IRolAutoridadRepository rolAutoridadRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AutoridadAcademicaServiceImpl(
            AutoridadAcademicaRepository autoridadRepository,
            UsuarioRepository usuarioRepository,
            InstitucionRepository institucionRepository,
            IRolAutoridadRepository rolAutoridadRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.autoridadRepository = autoridadRepository;
        this.usuarioRepository = usuarioRepository;
        this.institucionRepository = institucionRepository;
        this.rolAutoridadRepository = rolAutoridadRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public AutoridadAcademicaResponseDTO crear(AutoridadAcademicaRequestDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        RolAutoridad rolAutoridad = rolAutoridadRepository.findById(dto.getIdRolAutoridad())
                .orElseThrow(() -> new RuntimeException("Cargo (rol_autoridad) no encontrado"));

        // Crear autoridad
        AutoridadAcademica autoridad = new AutoridadAcademica();
        autoridad.setNombres(dto.getNombres());
        autoridad.setApellidos(dto.getApellidos());
        autoridad.setCorreo(dto.getCorreo());
        autoridad.setFechaNacimiento(dto.getFechaNacimiento());
        autoridad.setEstado(dto.getEstado());
        autoridad.setIdInstitucion(dto.getIdInstitucion());
        autoridad.setUsuario(usuario);
        autoridad.setRolAutoridad(rolAutoridad);

        autoridadRepository.save(autoridad);

        // Asignar roles al usuario según el cargo
        usuario.getRoles().addAll(rolAutoridad.getRolesUsuario());
        usuarioRepository.save(usuario);

        return mapToResponse(autoridad);
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
        autoridad.setEstado(dto.getEstado());
        autoridad.setUsuario(usuario);
        autoridad.setIdInstitucion(institucion.getIdInstitucion());

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
        dto.setEstado(autoridad.getEstado());
        dto.setIdUsuario(autoridad.getUsuario().getIdUsuario());
        dto.setIdInstitucion(autoridad.getIdInstitucion());
        return dto;
    }

    @Override
    public AutoridadRegistroResponseDTO registrarAutoridad(AutoridadRegistroRequestDTO dto) {

        institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        RolAutoridad rolAutoridad = rolAutoridadRepository.findById(dto.getIdRolAutoridad())
                .orElseThrow(() -> new RuntimeException("Cargo no encontrado"));

        // usuarioApp = prefijo del correo
        String usuarioApp = generarUsuarioAppDesdeCorreo(dto.getCorreo());

        // usuarioBd = nombres+apellidos (único)
        String baseBd = generarUsuarioBdBase(dto.getNombres(), dto.getApellidos());
        String usuarioBd = generarUsuarioBdUnico(baseBd);

        // clave temporal generada
        String claveTemporal = generarClaveTemporal(12);
        String claveHash = passwordEncoder.encode(claveTemporal);

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setUsuarioApp(usuarioApp);
        usuario.setClaveApp(claveHash);
        usuario.setCorreo(dto.getCorreo());
        usuario.setUsuarioBd(usuarioBd);
        usuario.setClaveBd("MTIzNA==");
        usuario.setActivo(true);

        // asignar roles según cargo
        usuario.getRoles().addAll(rolAutoridad.getRolesUsuario());

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Crear autoridad
        AutoridadAcademica autoridad = new AutoridadAcademica();
        autoridad.setNombres(dto.getNombres());
        autoridad.setApellidos(dto.getApellidos());
        autoridad.setCorreo(dto.getCorreo());
        autoridad.setFechaNacimiento(dto.getFechaNacimiento());
        autoridad.setEstado(true);
        autoridad.setIdInstitucion(dto.getIdInstitucion());
        autoridad.setUsuario(usuarioGuardado);
        autoridad.setRolAutoridad(rolAutoridad);

        AutoridadAcademica autoridadGuardada = autoridadRepository.save(autoridad);

        // enviar correo
        emailService.enviarCredenciales(dto.getCorreo(), usuarioApp, claveTemporal);

        AutoridadRegistroResponseDTO resp = new AutoridadRegistroResponseDTO();
        resp.setIdUsuario(usuarioGuardado.getIdUsuario());
        resp.setIdAutoridad(autoridadGuardada.getIdAutoridad());
        resp.setUsuarioApp(usuarioGuardado.getUsuarioApp());
        resp.setUsuarioBd(usuarioGuardado.getUsuarioBd());
        return resp;
    }

    private String generarUsuarioAppDesdeCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            throw new RuntimeException("Correo inválido para generar usuarioApp");
        }
        String base = correo.split("@")[0].trim().toLowerCase();
        base = base.replaceAll("\\s+", "").replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) throw new RuntimeException("No se pudo generar usuarioApp");

        String candidato = base;
        int n = 1;
        while (usuarioRepository.existsByUsuarioApp(candidato)) {
            n++;
            candidato = base + n;
        }
        return candidato;
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String t = s.toLowerCase();
        t = t.replaceAll("\\s+", "");
        t = t.replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ü","u")
                .replace("ñ","n");
        t = t.replaceAll("[^a-z0-9]", "");
        return t;
    }

    private String generarUsuarioBdBase(String nombres, String apellidos) {
        return normalizar(nombres) + normalizar(apellidos);
    }

    private String generarUsuarioBdUnico(String base) {
        if (base == null || base.isBlank()) throw new RuntimeException("No se pudo generar usuarioBd");
        String candidato = base;
        int n = 1;
        while (usuarioRepository.existsByUsuarioBd(candidato)) {
            n++;
            candidato = base + n;
        }
        return candidato;
    }

    private String generarClaveTemporal(int length) {
        final String ABC = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
        java.security.SecureRandom r = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ABC.charAt(r.nextInt(ABC.length())));
        return sb.toString();
    }

}
