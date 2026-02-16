package org.uteq.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MultipartConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir archivos estáticos desde el directorio uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
