package org.uteq.backend.config;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MutableDataSource extends AbstractDataSource {

    private final AtomicReference<DataSource> current = new AtomicReference<>();
    private final DataSource defaultDs;

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public MutableDataSource(DataSource initial) {
        this.defaultDs = initial;
        this.current.set(initial);
    }

    public void switchTo(DataSource newDataSource) {
        DataSource old = this.current.getAndSet(newDataSource);

        // Cierra el viejo pool después de que termine el request (delay corto)
        if (old != null && old != defaultDs && old instanceof HikariDataSource hikariOld) {
            scheduler.schedule(() -> {
                try { hikariOld.close(); } catch (Exception ignored) {}
            }, 2, TimeUnit.SECONDS);
        }
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