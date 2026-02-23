package org.uteq.backend.config;

import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class MutableDataSource extends AbstractDataSource {

    // ─── Un DataSource por hilo, no uno global ─────────────────
    private final ThreadLocal<DataSource> threadLocal = new ThreadLocal<>();
    private final DataSource defaultDataSource;

    public MutableDataSource(DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
    }

    /** Asigna el datasource solo para el hilo actual. */
    public void switchTo(DataSource ds) {
        threadLocal.set(ds);
    }

    /** Elimina el override del hilo actual — vuelve al default. */
    public void reset() {
        threadLocal.remove();
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource ds = threadLocal.get();
        return (ds != null ? ds : defaultDataSource).getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        DataSource ds = threadLocal.get();
        return (ds != null ? ds : defaultDataSource).getConnection(username, password);
    }
}