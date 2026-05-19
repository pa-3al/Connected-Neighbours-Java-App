package com.app.infrastructure.sync;

import com.app.domain.model.Category;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CategorySqliteGateway {

    private final DatabaseConfig databaseConfig;

    public CategorySqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, Category> loadCategoriesById(Connection connection) throws SQLException {
        Map<String, Category> map = new HashMap<>();
        String sql = "SELECT * FROM categories";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new Category(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        rs.getString("event_id"),
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertCategory(Connection connection, Category c) throws SQLException {
        String sql = """
            INSERT INTO categories (id, name, type, active, created_at, updated_at, event_id, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                name=excluded.name, type=excluded.type, active=excluded.active, 
                created_at=excluded.created_at, updated_at=excluded.updated_at, 
                event_id=excluded.event_id, last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, c.id());
            pstmt.setString(2, c.name());
            pstmt.setString(3, c.type());
            pstmt.setBoolean(4, c.active() != null ? c.active() : true);
            pstmt.setTimestamp(5, c.createdAt() != null ? Timestamp.valueOf(c.createdAt()) : null);
            pstmt.setTimestamp(6, c.updatedAt() != null ? Timestamp.valueOf(c.updatedAt()) : null);
            pstmt.setString(7, c.eventId());
            pstmt.setTimestamp(8, c.lastModified() != null ? Timestamp.valueOf(c.lastModified()) : null);
            pstmt.setString(9, c.syncStatus() != null ? c.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}