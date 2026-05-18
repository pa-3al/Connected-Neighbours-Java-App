package com.app.infrastructure.sync;

import com.app.domain.model.EventTag;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EventTagSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public EventTagSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, EventTag> loadEventTagsById(Connection connection) throws SQLException {
        Map<String, EventTag> map = new HashMap<>();
        String sql = "SELECT * FROM event_tags";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("name"), new EventTag(
                        rs.getString("name"),
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertEventTag(Connection connection, EventTag et) throws SQLException {
        String sql = """
            INSERT INTO event_tags (name, last_modified, sync_status)
            VALUES (?, ?, ?)
            ON CONFLICT(name) DO UPDATE SET 
                last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, et.name());
            pstmt.setTimestamp(2, et.lastModified() != null ? Timestamp.valueOf(et.lastModified()) : null);
            pstmt.setString(3, et.syncStatus() != null ? et.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}