package org.uteq.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el AuditContextInterceptor para todos los endpoints de la API.
 * Excluye rutas públicas que no tienen token JWT (login, registro, etc.)
 * ya que en esas rutas el SecurityContext no tiene usuario autenticado
 * y el interceptor no haría nada de todas formas.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditContextInterceptor auditContextInterceptor;

    public WebMvcConfig(AuditContextInterceptor auditContextInterceptor) {
        this.auditContextInterceptor = auditContextInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(auditContextInterceptor)
                .addPathPatterns("/api/**");
    }
}