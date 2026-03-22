package org.uteq.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.uteq.backend.service.AesCipherService;

@Component
@Order(2)
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private final JdbcTemplate jdbc;
    private final AesCipherService aesCipherService;

    public AdminInitializer(JdbcTemplate jdbc, AesCipherService aesCipherService) {
        this.jdbc             = jdbc;
        this.aesCipherService = aesCipherService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Si admin ya existe → skip total
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM usuario WHERE usuario_app = 'admin'",
                    Integer.class
            );
            if (count != null && count > 0) {
                log.info(">>> [AdminInitializer] admin ya existe, skip.");
                return;
            }
        } catch (Exception e) {
            log.warn(">>> [AdminInitializer] No se pudo verificar usuario admin: {}", e.getMessage());
            return;
        }

        log.info(">>> [AdminInitializer] Creando usuario admin...");
        try {
            // Clave de la app (BCrypt)
            String claveAppHash = new BCryptPasswordEncoder().encode("admin");

            // Clave real del usuario PostgreSQL
            String claveBdReal = "Admin2024$Ssdc!";

            // Clave cifrada con AES para guardar en columna clave_bd
            // AuthService llama aesCipherService.descifrar(usuario.getClaveBd())
            // por eso DEBE guardarse cifrada, no en texto plano
            String claveBdCifrada = aesCipherService.cifrar(claveBdReal);

            // SP retorna TABLE → usar query, no update
            jdbc.query(
                    "SELECT * FROM sp_registrar_usuario_simple(?,?,?,?,?,?,?::varchar[])",
                    rs -> {},
                    "admin",
                    claveAppHash,
                    "admin@uteq.edu.ec",
                    "admin",
                    claveBdCifrada,   // columna clave_bd → cifrada AES
                    claveBdReal,      // para CREATE USER postgres → texto plano
                    new String[]{"ADMIN"}
            );

            // Desactivar primer_login para admin
            jdbc.update("UPDATE usuario SET primer_login = false WHERE usuario_app = 'admin'");

            log.info(">>> [AdminInitializer] Usuario admin creado correctamente.");
        } catch (Exception e) {
            log.error(">>> [AdminInitializer] Error creando admin: {}", e.getMessage());
        }
    }
}