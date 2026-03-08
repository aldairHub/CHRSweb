package org.uteq.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = {
        "org.uteq.backend.config",
        "org.uteq.backend.controller",
        "org.uteq.backend.service",
        "org.uteq.backend.repository"
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Tomcat 11 tiene un límite bajo de partes multipart (fileCountMax).
     * Esto lo sube a 500 para permitir formularios con múltiples archivos.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatFileCountCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector ->
                connector.setProperty("fileCountMax", "500")
        );
    }
}
