package org.uteq.backend.Config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    public MutableDataSource dataSource() {

        // CAMBIAR conexión inicial base (admin backend)
        HikariDataSource base = new HikariDataSource();
        base.setJdbcUrl("jdbc:postgresql://localhost:5432/ssdc_DB");
        base.setUsername("postgres");
        base.setPassword("admin");

        return new MutableDataSource(base);
    }
}