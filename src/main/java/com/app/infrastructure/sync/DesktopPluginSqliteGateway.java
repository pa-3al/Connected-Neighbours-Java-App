package com.app.infrastructure.sync;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import com.app.infrastructure.adapter.persistence.DatabaseConfig;

public class DesktopPluginSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public DesktopPluginSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

}