package org.uteq.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

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
}
