package org.uteq.backend.service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uteq.backend.config.MutableDataSource;

@Service
public class DbSwitchService {

    private final MutableDataSource mutable;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    public DbSwitchService(MutableDataSource mutable) {
        this.mutable = mutable;
    }

    public void switchToUser(String usuarioBd, String claveBd) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(usuarioBd);
        ds.setPassword(claveBd);
        ds.setMaximumPoolSize(2);
        mutable.switchTo(ds);
        System.out.println("Hilo " + Thread.currentThread().getId() +
                " conectado como: " + usuarioBd);
    }

    public void resetToDefault() {
        mutable.reset();
        System.out.println("Hilo " + Thread.currentThread().getId() +
                " devuelto al datasource default");
    }
}