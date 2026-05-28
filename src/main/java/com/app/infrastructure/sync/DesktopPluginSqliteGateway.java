package com.app.infrastructure.sync;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.app.domain.model.PluginMetadata;
import com.app.domain.model.PluginOrigin;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

public class DesktopPluginSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public DesktopPluginSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public List<PluginMetadata> loadDesktopPlugins() {
        try (Connection connection = openLocalConnection()) {
            return loadDesktopPlugins(connection);
        } catch (SQLException e) {
            return List.of();
        }
    }

    public List<PluginMetadata> loadDesktopPlugins(Connection connection) throws SQLException {
        List<PluginMetadata> plugins = new ArrayList<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM desktop_plugins WHERE is_loaded = 0")) {
            while (rs.next()) {
                plugins.add(readPlugin(rs));
            }
        }
        return plugins;
    }
}