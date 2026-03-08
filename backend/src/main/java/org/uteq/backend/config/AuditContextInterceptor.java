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
 * Interceptor que, en cada request HTTP autenticado, ejecuta:
 *
 *   SELECT set_config('app.usuario_app', '<correo>', false)
 *   SELECT set_config('app.ip_cliente',  '<ip>',    false)
 *
 * Esto deja las variables disponibles para toda la sesión PostgreSQL
 * de ese request. Los triggers fn_auditar_accion y fn_auditar_usuario
 * las leen con current_setting('app.usuario_app', true) en lugar de
 * hardcodear 'db_directo'.
 *
 * Se usa false (no local) porque los triggers corren dentro de la misma
 * transacción/conexión y necesitan ver la variable.
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

            try {
                // Setea variables de sesión PostgreSQL que los triggers leerán
                jdbcTemplate.queryForObject(
                        "SELECT set_config('app.usuario_app', ?, false)",
                        String.class, usuarioApp
                );
                jdbcTemplate.queryForObject(
                        "SELECT set_config('app.ip_cliente', ?, false)",
                        String.class, ip != null ? ip : "desconocida"
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