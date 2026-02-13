package org.uteq.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults()) // aquí: habilita CORS usando tu CorsConfigurationSource
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/usuarios",
                                "/api/auth/login",
                                "/api/registro/**",
                                "/api/prepostulacion/**",
                                "/api/carreras/**",
                                "/api/facultades/**",
                                "/api/facultades",
                                "/api/materias/**",
                                "/api/materias",
                                "/uploads/**",
                                "/api/verificacion/enviar",
                                "/api/verificacion/validar",
                                "/api/autoridades-academicas/**",
                                "/api/autoridades-academicas/registro",
                                "/api/admin/prepostulaciones/**",
                                "/api/instituciones",
                                "/api/instituciones/**",
                                "/api/roles-autoridad",
                                "/api/roles-autoridad/**",
                                "/api/roles-usuario",
                                "/api/roles-usuario/**"
                                ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/demo/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:8080",
                "http://localhost:4200"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}