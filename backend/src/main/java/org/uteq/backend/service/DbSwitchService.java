package org.uteq.backend.service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uteq.backend.config.MutableDataSource;

import javax.sql.DataSource;

@Service
public class DbSwitchService {

    private final MutableDataSource mutable;
    private final DataSource defaultDs;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    public DbSwitchService(MutableDataSource mutable,
                           @Qualifier("baseHikariDataSource") DataSource defaultDs) {
        this.mutable = mutable;
        this.defaultDs = defaultDs;
    }

    public void switchToUser(String usuarioBd, String claveBd) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(usuarioBd);
        ds.setPassword(claveBd);

        ds.setMaximumPoolSize(1);
        ds.setMinimumIdle(0);
        ds.setIdleTimeout(15000);
        ds.setMaxLifetime(180000);
        ds.setConnectionTimeout(30000);

        ds.addDataSourceProperty("ApplicationName", "backend-" + usuarioBd);

        mutable.switchTo(ds);
        System.out.println("Backend conectado ahora como usuario BD: " + usuarioBd);
    }

    public void resetToDefault() {
        mutable.switchTo(defaultDs);
        System.out.println("Conexión devuelta al usuario backend default");
    }
}