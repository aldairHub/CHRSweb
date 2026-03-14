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

    /**
     * Nombre del atributo del request donde se guarda el usuarioBd extraído del JWT.
     * AuditContextInterceptor lo lee de aquí para inyectarlo en app.usuario_bd.
     */
    public static final String ATTR_USUARIO_BD = "jwt_usuario_bd";

    private final JwtService                jwtService;
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
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Token expirado\",\"code\":\"TOKEN_EXPIRED\"}");
            return;
        }

        String  username = jwtService.extractUsername(token);
        Integer tvToken  = jwtService.extractTokenVersion(token);

        // Verificar versión solo si el token trae el claim "tv"
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

        // Extraer usuarioBd del claim y dejarlo en el request para AuditContextInterceptor.
        String usuarioBd = jwtService.extractUsuarioBd(token);
        if (usuarioBd != null) {
            request.setAttribute(ATTR_USUARIO_BD, usuarioBd);
        }

        List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}