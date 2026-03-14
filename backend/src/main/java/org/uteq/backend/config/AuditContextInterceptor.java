package org.uteq.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que inyecta variables de sesión PostgreSQL en cada request autenticado.
 *
 * Variables que se inyectan:
 *   app.usuario_app  → correo del JWT (auth.getName())
 *   app.ip_cliente   → IP real del cliente HTTP
 *   app.usuario_bd   → usuario de PostgreSQL del JWT (claim "usuario_bd")
 *
 * Los triggers de auditoría (fn_aud_cambio) leen estas variables con
 * current_setting('app.usuario_bd', true) para saber quién hizo el cambio,
 * incluso cuando la operación pasa por una función SECURITY DEFINER
 * (donde current_user y session_user no son confiables con connection pooling).
 *
 * Si el token no tiene el claim usuario_bd (tokens anteriores al cambio),
 * app.usuario_bd queda como cadena vacía — el trigger lo lee como NULL.
 */
@Component
public class AuditContextInterceptor implements HandlerInterceptor {

    private final JdbcTemplate jdbcTemplate;

    public AuditContextInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {

            String usuarioApp = auth.getName();
            String ip         = obtenerIp(request);

            // usuarioBd viene del claim del JWT, extraído y guardado por JwtAuthFilter
            String usuarioBd = (String) request.getAttribute(JwtAuthFilter.ATTR_USUARIO_BD);

            try {
                jdbcTemplate.queryForObject(
                        "SELECT set_config('app.usuario_app', ?, false)",
                        String.class, usuarioApp
                );
                jdbcTemplate.queryForObject(
                        "SELECT set_config('app.ip_cliente', ?, false)",
                        String.class, ip != null ? ip : "desconocida"
                );
                jdbcTemplate.queryForObject(
                        "SELECT set_config('app.usuario_bd', ?, false)",
                        String.class, usuarioBd != null ? usuarioBd : ""
                );
            } catch (Exception e) {
                // No bloquear el request si falla el set_config
            }
        }

        return true;
    }

    private String obtenerIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}