package com.app.infrastructure.adapter.persistence;

import com.app.infrastructure.config.ConfigProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseConfig() {
        ConfigProvider configProvider = new ConfigProvider();
        dbUrl = configProvider.getDatabaseUrl();
        dbUser = configProvider.getDatabaseUser();
        dbPassword = configProvider.getDatabasePassword();
    }

    public Connection getConnection() throws SQLException {
        if (dbUser == null || dbUser.isBlank()) {
            return DriverManager.getConnection(dbUrl);
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}