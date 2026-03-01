package org.uteq.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.service.JwtService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PostgresProcedureRepository procedureRepository;

    public JwtAuthFilter(JwtService jwtService,
                         PostgresProcedureRepository procedureRepository) {
        this.jwtService          = jwtService;
        this.procedureRepository = procedureRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response); return;
        }

        String  username = jwtService.extractUsername(token);
        Integer tvToken  = jwtService.extractTokenVersion(token);

        // Verificar versión solo si el token trae el claim "tv"
        // Tokens emitidos antes de este cambio no tienen "tv" → pasan
        if (tvToken != null) {
            Integer tvBd = procedureRepository.obtenerTokenVersion(username);
            if (tvBd == null || !tvBd.equals(tvToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Sesión expirada. Inicia sesión nuevamente.\"}");
                return;
            }
        }

        List<String> roles = jwtService.extractRoles(token);
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
