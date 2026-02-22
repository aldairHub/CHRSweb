package org.uteq.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.UsuarioCreateDTO;
import org.uteq.backend.dto.UsuarioDTO;
import org.uteq.backend.dto.UsuarioUpdateDTO;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.dto.CambiarClaveDTO;
import org.uteq.backend.util.CredencialesGenerator;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PostgresProcedureRepository procedureRepository; // ✅ nuevo


    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          PostgresProcedureRepository procedureRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.procedureRepository = procedureRepository;

    }

    public UsuarioDTO crear(UsuarioCreateDTO dto) {

        if (dto.getUsuarioApp() == null || dto.getUsuarioApp().isBlank()) {
            throw new RuntimeException("usuarioApp es obligatorio");
        }

        if (dto.getClaveApp() == null || dto.getClaveApp().isBlank()) {
            throw new RuntimeException("claveApp es obligatoria");
        }

        if (dto.getUsuarioBd() == null || dto.getUsuarioBd().isBlank()) {
            throw new RuntimeException("usuarioBd es obligatorio");
        }

        if (dto.getClaveBd() == null || dto.getClaveBd().isBlank()) {
            throw new RuntimeException("claveBdBase64 es obligatoria");
        }

        if (dto.getCorreo() == null || dto.getCorreo().isBlank()) {
            throw new RuntimeException("correo es obligatorio");
        }

        if (usuarioRepository.findByUsuarioApp(dto.getUsuarioApp()).isPresent()) {
            throw new RuntimeException("El usuario ya existe");
        }

        Usuario usuario = new Usuario();
        usuario.setUsuarioBd(dto.getUsuarioBd());

        usuario.setClaveBd(dto.getClaveBd());

        usuario.setUsuarioApp(dto.getUsuarioApp());
        usuario.setClaveApp(passwordEncoder.encode(dto.getClaveApp())); // BCrypt
        usuario.setCorreo(dto.getCorreo());
        usuario.setActivo(true);
        //usuario.setRol(dto.getRol());

        Usuario guardado = usuarioRepository.save(usuario);
        return convertirADTO(guardado);
    }

    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }


    public UsuarioDTO obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return convertirADTO(usuario);
    }

    public UsuarioDTO actualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


        if (dto.getUsuarioBd() != null) {
            usuario.setUsuarioBd(dto.getUsuarioBd());
        }
        if (dto.getClaveBd() != null) {
            usuario.setClaveBd(dto.getClaveBd());
        }
        if (dto.getUsuarioApp() != null) {
            usuario.setUsuarioApp(dto.getUsuarioApp());
        }
        if (dto.getClaveApp() != null && !dto.getClaveApp().isEmpty()) {
            usuario.setClaveApp(passwordEncoder.encode(dto.getClaveApp()));
        }
        if (dto.getActivo() != null) {
            usuario.setActivo(dto.getActivo());
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return convertirADTO(actualizado);
    }

    public UsuarioDTO cambiarEstado(Long id, Boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(activo);
        Usuario actualizado = usuarioRepository.save(usuario);
        return convertirADTO(actualizado);
    }

    private UsuarioDTO convertirADTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setUsuarioBd(usuario.getUsuarioBd());
        dto.setUsuarioApp(usuario.getUsuarioApp());
        dto.setCorreo(usuario.getCorreo());           // agregar
        dto.setActivo(usuario.getActivo());
        dto.setPrimerLogin(usuario.getPrimerLogin()); //  agregar
        return dto;
    }
    // ─── Caso 1: Primer login ───────────────────────────────────

    @Transactional
    public void cambiarClavePrimerLogin(String usuarioApp, CambiarClaveDTO dto) {
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(usuario.getPrimerLogin()))
            throw new RuntimeException("Este usuario ya realizó su primer cambio de clave");

        validarClaveNueva(dto, usuario);

        //  SP con SECURITY DEFINER — resuelve problema de permisos
        String hash = passwordEncoder.encode(dto.getClaveNueva());
        procedureRepository.cambiarClaveApp(usuarioApp, hash);
    }

    // ─── Caso 2: Cambio voluntario ─────────────────────────────

    @Transactional
    public void cambiarClave(String usuarioApp, CambiarClaveDTO dto) {
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getClaveActual(), usuario.getClaveApp()))
            throw new RuntimeException("La contraseña actual es incorrecta");

        validarClaveNueva(dto, usuario);

        //  SP con SECURITY DEFINER
        String hash = passwordEncoder.encode(dto.getClaveNueva());
        procedureRepository.cambiarClaveApp(usuarioApp, hash);
    }

    // ─── Caso 3: Olvidó contraseña ─────────────────────────────

    @Transactional
    public void recuperarClave(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("No existe cuenta con ese correo"));

        if (!Boolean.TRUE.equals(usuario.getActivo()))
            throw new RuntimeException("La cuenta está inactiva");

        String claveTemporal = CredencialesGenerator.generarClaveApp();
        String hash          = passwordEncoder.encode(claveTemporal);

        // SP con SECURITY DEFINER — setea primer_login = true
        procedureRepository.recuperarClaveApp(usuario.getUsuarioApp(), hash);

        try {
            emailService.enviarCredenciales(correo, usuario.getUsuarioApp(), claveTemporal);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar el correo de recuperación");
        }
    }

    // ─── Helpers ───────────────────────────────────────────────

    private void validarClaveNueva(CambiarClaveDTO dto, Usuario usuario) {
        if (dto.getClaveNueva() == null || dto.getClaveNueva().isBlank())
            throw new RuntimeException("La nueva contraseña no puede estar vacía");
        if (dto.getClaveNueva().length() < 8)
            throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres");
        if (!dto.getClaveNueva().equals(dto.getClaveNuevaConfirmacion()))
            throw new RuntimeException("La nueva contraseña y su confirmación no coinciden");
        if (passwordEncoder.matches(dto.getClaveNueva(), usuario.getClaveApp()))
            throw new RuntimeException("La nueva contraseña debe ser diferente a la actual");
    }
}