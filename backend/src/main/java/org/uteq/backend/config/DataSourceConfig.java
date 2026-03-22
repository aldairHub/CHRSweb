package org.uteq.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean(name = "baseHikariDataSource", destroyMethod = "close")
    public HikariDataSource baseHikariDataSource(
            @Value("${spring.datasource.url}")      String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${backup.postgres.username:postgres}") String pgAdmin,
            @Value("${backup.postgres.password:admin}")    String pgPass
    ) {
        // ── Auto-crear la BD si no existe ──────────────────────────
        crearBaseDeDatosIfNotExists(url, pgAdmin, pgPass);
        // ───────────────────────────────────────────────────────────

        HikariDataSource base = new HikariDataSource();
        base.setJdbcUrl(url);
        base.setUsername(username);
        base.setPassword(password);
        base.setMaximumPoolSize(2);
        base.setMinimumIdle(1);
        base.setIdleTimeout(15000);
        base.setMaxLifetime(180000);
        base.setConnectionTimeout(30000);
        base.addDataSourceProperty("ApplicationName", "backend-default");
        return base;
    }

    @Bean
    @Primary
    public MutableDataSource dataSource(HikariDataSource baseHikariDataSource) {
        log.info("DataSource conectado a: {}", baseHikariDataSource.getJdbcUrl());
        return new MutableDataSource(baseHikariDataSource);
    }

    /**
     * Extrae el nombre de la BD del JDBC URL y la crea si no existe.
     * Se conecta a la BD "postgres" (siempre existe) como superusuario.
     *
     * Ejemplo URL: jdbc:postgresql://localhost:5432/ssdc_loc
     *   → se conecta a  jdbc:postgresql://localhost:5432/postgres
     *   → verifica si "ssdc_loc" existe
     *   → si no existe, ejecuta CREATE DATABASE ssdc_loc
     */
    private void crearBaseDeDatosIfNotExists(String url, String pgAdmin, String pgPass) {
        try {
            // Extraer nombre de BD y host del URL
            // url = jdbc:postgresql://host:port/dbname[?params]
            String sinJdbc   = url.replace("jdbc:postgresql://", "");
            String[] partes  = sinJdbc.split("/", 2);
            String hostPort  = partes[0];                          // localhost:5432
            String dbConParams = partes.length > 1 ? partes[1] : "";
            String dbName    = dbConParams.split("\\?")[0].trim(); // ssdc_loc

            String urlPostgres = "jdbc:postgresql://" + hostPort + "/postgres";

            log.info(">>> Verificando si la BD '{}' existe...", dbName);

            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(urlPostgres, pgAdmin, pgPass);
                 Statement  stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery(
                        "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'"
                );

                if (!rs.next()) {
                    log.info(">>> BD '{}' no existe — creándola automáticamente...", dbName);
                    // CREATE DATABASE no puede ejecutarse en transacción
                    conn.setAutoCommit(true);
                    stmt.execute("CREATE DATABASE \"" + dbName + "\"");
                    log.info(">>> BD '{}' creada correctamente ✅", dbName);
                } else {
                    log.info(">>> BD '{}' ya existe, continuando...", dbName);
                }
            }
        } catch (Exception e) {
            log.warn(">>> No se pudo verificar/crear la BD automáticamente: {}", e.getMessage());
            log.warn(">>> Asegúrate de que backup.postgres.username/password en application.properties son correctos.");
        }
    }
}