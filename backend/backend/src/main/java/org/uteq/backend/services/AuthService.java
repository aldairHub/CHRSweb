package org.uteq.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import org.uteq.backend.entities.Usuario;
import org.uteq.backend.repositories.UsuarioRepository;

@Service
public class AuthService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    public LoginResponse login(LoginRequest loginRequest) {
        Usuario usuario = usuarioRepository
                .findByUsuarioAppAndClaveAppAndActivoTrue(
                        loginRequest.getUsuarioApp(),
                        loginRequest.getClaveApp()
                )
                .orElse(null);
        if (usuario == null) {
            return new LoginResponse(null, null, null, null, "Sus credenciales son incorrectas");
        }
        String nombreRol = usuario.getRoles() != null ? usuario.getRoles().getNombre() : "Este usuario tiene un rol asignado";
        Long idRol = usuario.getRoles() != null ? usuario.getRoles().getId_rol() : null;

        return new LoginResponse(
                usuario.getId_usuario(),
                usuario.getUsuarioApp(),
                nombreRol,
                idRol,
                "Login ejecutado con exito"
        );

    }
}
