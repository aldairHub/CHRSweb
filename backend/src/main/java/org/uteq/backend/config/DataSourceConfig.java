package org.uteq.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    @Bean(name = "baseHikariDataSource", destroyMethod = "close")
    public HikariDataSource baseHikariDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password
    ) {
        HikariDataSource base = new HikariDataSource();
        base.setJdbcUrl(url);
        base.setUsername(username);
        base.setPassword(password);

        // Supabase-friendly
        base.setMaximumPoolSize(2);
        base.setMinimumIdle(1);
        base.setIdleTimeout(15000);
        base.setMaxLifetime(180000);
        base.setConnectionTimeout(30000);

        base.addDataSourceProperty("ApplicationName", "backend-default");
        return base;
    }

    @Bean
    @Primary // <<< CLAVE: este será el DataSource que usará JPA
    public MutableDataSource dataSource(HikariDataSource baseHikariDataSource) {
        System.out.println("DEBUG: Intentando conectar a " + baseHikariDataSource.getJdbcUrl() + " con usuario " + baseHikariDataSource.getUsername());

        return new MutableDataSource(baseHikariDataSource);
    }
}