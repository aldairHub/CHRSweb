package org.uteq.backend.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Repository.UsuarioRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public AppUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByUsuarioApp(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // OJO: authorities reales vienen del JWT, aquí basta con username/password
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsuarioApp())
                .password(u.getClaveApp())
                .disabled(!u.getActivo())
                .authorities(new java.util.ArrayList<>())
                .build();
    }
}