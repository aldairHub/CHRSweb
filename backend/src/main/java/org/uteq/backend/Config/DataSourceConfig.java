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
        base.setJdbcUrl("jdbc:postgresql://localhost:5432/ssdcTest");
        base.setUsername("postgres");
        base.setPassword("postgresqlAdmin19");

        return new MutableDataSource(base);
    }
}