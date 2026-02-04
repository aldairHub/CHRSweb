package org.uteq.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.UsuarioCreateDTO;
import org.uteq.backend.dto.UsuarioDTO;
import org.uteq.backend.dto.UsuarioUpdateDTO;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Repository.UsuarioRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

//    public List<UsuarioDTO> listarAutoridades() {
//        return usuarioRepository.findAll().stream()
//                .filter(u -> u.getRol() == Role.ADMIN || u.getRol() == Role.EVALUATOR)
//                .map(this::convertirADTO)
//                .collect(Collectors.toList());
//    }

    public UsuarioDTO obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return convertirADTO(usuario);
    }

    public UsuarioDTO actualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

//        if (dto.getRol() == Role.USER) {
//            throw new RuntimeException("No se permite cambiar el rol a USER manualmente");
//        }

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
//        if (dto.getRol() != null) {
//            usuario.setRol(dto.getRol());
//        }

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
        dto.setActivo(usuario.getActivo());
        //dto.setRol(usuario.getRol());
        return dto;
    }
}