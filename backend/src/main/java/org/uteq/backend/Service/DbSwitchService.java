package org.uteq.backend.Service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;
import org.uteq.backend.Config.MutableDataSource;

@Service
public class DbSwitchService {

    private final MutableDataSource mutable;

    public DbSwitchService(MutableDataSource mutable) {
        this.mutable = mutable;
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
}