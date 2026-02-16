package org.uteq.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
    // Esto lee automáticamente de tu application.properties
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    public MutableDataSource dataSource() {
        HikariDataSource base = new HikariDataSource();
        base.setJdbcUrl(url);
        base.setUsername(username);
        base.setPassword(password);
        System.out.println("DEBUG: Intentando conectar a " + base.getJdbcUrl() + " con usuario " + base.getUsername());
        return new MutableDataSource(base);
    }
}