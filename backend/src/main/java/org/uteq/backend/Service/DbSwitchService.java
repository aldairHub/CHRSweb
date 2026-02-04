package org.uteq.backend.Service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;
import org.uteq.backend.Config.MutableDataSource;

import javax.sql.DataSource;

@Service
public class DbSwitchService {

    private final MutableDataSource mutable;
    private final DataSource defaultDs;


    public DbSwitchService(MutableDataSource mutable) {
        this.mutable = mutable;

        // Default fijo (igual que en DataSourceConfig)
        HikariDataSource base = new HikariDataSource();
        base.setJdbcUrl("jdbc:postgresql://localhost:5432/ssdc_DB");
        base.setUsername("postgres");
        base.setPassword("admin");
        base.setMaximumPoolSize(5);

        this.defaultDs = base;
    }
    public void switchToUser(String usuarioBd, String claveBd) {

        HikariDataSource ds = new HikariDataSource();
        //CAMBIAR
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/ssdc_practice");
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