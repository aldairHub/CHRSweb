package org.uteq.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.UsuarioCreateDTO;
import org.uteq.backend.dto.UsuarioDTO;
import org.uteq.backend.dto.UsuarioUpdateDTO;
import org.uteq.backend.entity.Usuario;
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

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public UsuarioDTO crear(UsuarioCreateDTO dto) {
//        if (dto.getRol() == null) {
//            throw new RuntimeException("Debe especificar un rol");
//        }

//        if (dto.getRol() == Role.USER) {
//            throw new RuntimeException("No se permite crear usuarios con rol USER manualmente");
//        }

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
        dto.setCorreo(usuario.getCorreo());           // ✅ agregar
        dto.setActivo(usuario.getActivo());
        dto.setPrimerLogin(usuario.getPrimerLogin()); // ✅ agregar
        return dto;
    }
    // ─── Caso 1: Primer login — cambio obligatorio ─────────────

    @Transactional
    public void cambiarClavePrimerLogin(String usuarioApp, CambiarClaveDTO dto) {

        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que realmente es primer login
        if (!Boolean.TRUE.equals(usuario.getPrimerLogin())) {
            throw new RuntimeException("Este usuario ya realizó su primer cambio de clave");
        }

        validarClaveNueva(dto, usuario);

        usuario.setClaveApp(passwordEncoder.encode(dto.getClaveNueva()));
        usuario.setPrimerLogin(false); //  marcar que ya no es primer login
        usuarioRepository.save(usuario);
    }

    // ─── Caso 2: Cambio voluntario ─────────────────────────────

    @Transactional
    public void cambiarClave(String usuarioApp, CambiarClaveDTO dto) {

        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Requiere clave actual
        if (!passwordEncoder.matches(dto.getClaveActual(), usuario.getClaveApp())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        validarClaveNueva(dto, usuario);

        usuario.setClaveApp(passwordEncoder.encode(dto.getClaveNueva()));
        usuario.setPrimerLogin(false);
        usuarioRepository.save(usuario);
    }

    // ─── Caso 3: Olvidó contraseña ─────────────────────────────

    @Transactional
    public void recuperarClave(String correo) {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("No existe una cuenta con ese correo"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new RuntimeException("La cuenta está inactiva");
        }

        // Generar clave temporal nueva
        String claveTemporal = CredencialesGenerator.generarClaveApp();

        // Actualizar en BD
        usuario.setClaveApp(passwordEncoder.encode(claveTemporal));
        usuario.setPrimerLogin(true); // ✅ forzar cambio al entrar
        usuarioRepository.save(usuario);

        // Enviar correo con clave temporal
        try {
            emailService.enviarCredenciales(
                    correo,
                    usuario.getUsuarioApp(),
                    claveTemporal
            );
        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar el correo de recuperación");
        }
    }

    // ─── Validaciones comunes ───────────────────────────────────

    private void validarClaveNueva(CambiarClaveDTO dto, Usuario usuario) {

        if (dto.getClaveNueva() == null || dto.getClaveNueva().isBlank()) {
            throw new RuntimeException("La nueva contraseña no puede estar vacía");
        }

        if (dto.getClaveNueva().length() < 8) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        if (!dto.getClaveNueva().equals(dto.getClaveNuevaConfirmacion())) {
            throw new RuntimeException("La nueva contraseña y su confirmación no coinciden");
        }

        if (passwordEncoder.matches(dto.getClaveNueva(), usuario.getClaveApp())) {
            throw new RuntimeException("La nueva contraseña debe ser diferente a la actual");
        }
    }
}