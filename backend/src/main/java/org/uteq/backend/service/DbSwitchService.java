package org.uteq.backend.service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uteq.backend.config.MutableDataSource;

import javax.sql.DataSource;

@Service
public class DbSwitchService {

    private final MutableDataSource mutable;
    private final DataSource defaultDs;

    // Leemos las propiedades del archivo application.properties
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public DbSwitchService(MutableDataSource mutable,
                           @Value("${spring.datasource.url}") String url,
                           @Value("${spring.datasource.username}") String user,
                           @Value("${spring.datasource.password}") String pass) {
        this.mutable = mutable;

        // Usamos los parámetros inyectados para el default
        HikariDataSource base = new HikariDataSource();
        base.setJdbcUrl(url);
        base.setUsername(user);
        base.setPassword(pass);
        base.setMaximumPoolSize(5);

        this.defaultDs = base;
    }

    public void switchToUser(String usuarioBd, String claveBd) {
        HikariDataSource ds = new HikariDataSource();

        // Usamos la URL del properties pero con las credenciales dinámicas
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(usuarioBd);
        ds.setPassword(claveBd);

        ds.setMaximumPoolSize(2);

        mutable.switchTo(ds);

        System.out.println("Backend conectado ahora como usuario BD: " + usuarioBd);
    }

    public void resetToDefault() {
        mutable.switchTo(defaultDs);
        System.out.println("Conexión devuelta al usuario backend default");
    }
}