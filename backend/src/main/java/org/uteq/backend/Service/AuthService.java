package org.uteq.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Repository.UsuarioRepository;

import java.util.Collections;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsuarioApp(request.getUsuarioApp())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getClaveApp(), usuario.getClaveApp())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        if (!usuario.getActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }

        String token = jwtService.generateToken(usuario);

        return new LoginResponse(token, usuario.getUsuarioApp(),
                Collections.singleton(usuario.getRol().name()));
    }
}