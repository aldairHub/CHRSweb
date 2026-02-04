package org.uteq.backend.Config;

import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public class MutableDataSource extends AbstractDataSource {

    private final AtomicReference<DataSource> current = new AtomicReference<>();

    public MutableDataSource(DataSource initial) {
        this.current.set(initial);
    }

    public void switchTo(DataSource newDataSource) {
        this.current.set(newDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return current.get().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return current.get().getConnection(username, password);
    }
}