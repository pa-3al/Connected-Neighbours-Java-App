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

    public void replaceDesktopPlugins(Collection<PluginMetadata> plugins) throws SQLException {
        try (Connection connection = openLocalConnection()) {
            connection.setAutoCommit(false);
            
            List<String> loadedPlugins = new ArrayList<>();
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id FROM desktop_plugins WHERE is_loaded = 1")) {
                while (rs.next()) {
                    loadedPlugins.add(rs.getString("id"));
                }
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM desktop_plugins");
            }
            String sql = """
                INSERT INTO desktop_plugins (id, name, version, author, description, enabled, download_url, source, is_loaded, last_modified, sync_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (PluginMetadata plugin : plugins) {
                    pstmt.setString(1, plugin.id());
                    pstmt.setString(2, plugin.name());
                    pstmt.setString(3, plugin.version());
                    pstmt.setString(4, plugin.author());
                    pstmt.setString(5, plugin.description());
                    pstmt.setBoolean(6, plugin.enabled());
                    pstmt.setString(7, plugin.downloadUrl());
                    pstmt.setString(8, plugin.source() != null ? plugin.source().name() : PluginOrigin.REMOTE_CATALOG.name());
                    pstmt.setBoolean(9, loadedPlugins.contains(plugin.id()) || plugin.isLoaded());
                    pstmt.setTimestamp(10, null);
                    pstmt.setString(11, null);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            connection.commit();
        }
    }
}