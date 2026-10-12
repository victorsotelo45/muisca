package com.unicauca.muisca.infrastructure.config;

import com.unicauca.muisca.domain.repository.ExecutionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Ajustes de esquema que ddl-auto=update no hace por sí mismo. Se ejecuta antes de sembrar datos.
 * <ul>
 * <li>MySQL: Hibernate creó executions.status como ENUM('FAILED','SUCCEEDED'); se pasa a VARCHAR
 * para admitir el estado RUNNING de las ejecuciones en segundo plano.</li>
 * <li>Ejecuciones que quedaron RUNNING por un reinicio del servidor se marcan como fallidas.</li>
 * </ul>
 */
@Component
@Order(0)
public class DatabaseMigrations implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrations.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final ExecutionRepository executionRepository;

    public DatabaseMigrations(DataSource dataSource, ExecutionRepository executionRepository) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.executionRepository = executionRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        if (isMySql()) {
            widenMySqlEnum("executions", "status", "VARCHAR(20) NOT NULL");
        }
        int interrupted = executionRepository.failInterrupted(
                "La ejecución se interrumpió porque el servidor se reinició. Ejecútela de nuevo.");
        if (interrupted > 0) {
            log.warn("{} ejecuciones en curso se marcaron como fallidas por el reinicio", interrupted);
        }
    }

    private boolean isMySql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        }
    }

    private void widenMySqlEnum(String table, String column, String definition) {
        List<String> types = jdbc.queryForList("select data_type from information_schema.columns "
                + "where table_schema = database() and table_name = ? and column_name = ?", String.class, table, column);
        if (!types.isEmpty() && "enum".equalsIgnoreCase(types.get(0))) {
            jdbc.execute("alter table " + table + " modify " + column + " " + definition);
            log.info("Columna {}.{} convertida de ENUM a {}", table, column, definition);
        }
    }
}
