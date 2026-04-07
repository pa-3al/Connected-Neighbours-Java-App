package com.app.infrastructure.adapter.persistence;

import com.app.infrastructure.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void databaseConfigShouldReturnValidSqliteConnectionFromProperties() {
        ConfigProvider configProvider = new ConfigProvider();
        String dbUrl = configProvider.getDatabaseUrl();

        try {
            Connection connection = DriverManager.getConnection(dbUrl);
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            connection.close();
        } catch (SQLException e) {
            fail("Erreur SQLite: " + e.getMessage());
        }
    }

    @Test
    void databaseConfigShouldReturnValidPostgresConnectionFromProperties() {
        ConfigProvider configProvider = new ConfigProvider();
        String rawUrl = configProvider.getSyncDatabaseUrl();

        String jdbcUrl = rawUrl;
        if (jdbcUrl != null && !jdbcUrl.startsWith("jdbc:")) {
            jdbcUrl = "jdbc:" + jdbcUrl;
        }

        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);

            assertNotNull(connection);
            assertFalse(connection.isClosed());
            connection.close();
        } catch (SQLException e) {
            fail("La connexion à PostgreSQL a échoué. Vérifiez que le serveur est accessible et que les identifiants sont corrects. Erreur : " + e.getMessage());
        }
    }
}