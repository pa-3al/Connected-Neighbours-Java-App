package com.app.infrastructure.adapter.persistence;

import com.app.infrastructure.config.ConfigProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        ensureSqliteParentDirectory();
        if (dbUser == null || dbUser.isBlank()) {
            return DriverManager.getConnection(dbUrl);
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private void ensureSqliteParentDirectory() throws SQLException {
        if (dbUrl == null || !dbUrl.startsWith("jdbc:sqlite:")) {
            return;
        }

        String pathValue = dbUrl.substring("jdbc:sqlite:".length());
        if (pathValue.isBlank() || pathValue.equals(":memory:") || pathValue.startsWith("file:")) {
            return;
        }

        Path parent = Paths.get(pathValue).toAbsolutePath().normalize().getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (java.io.IOException e) {
            throw new SQLException("Unable to create SQLite database directory: " + parent, e);
        }
    }
}
