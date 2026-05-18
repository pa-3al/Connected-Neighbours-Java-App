package com.app.infrastructure.sync;

import com.app.domain.model.Media;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MediaSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public MediaSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, Media> loadMediaById(Connection connection) throws SQLException {
        Map<String, Media> map = new HashMap<>();
        String sql = "SELECT * FROM media";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new Media(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getString("url"),
                        rs.getString("file_extension"),
                        rs.getString("neighbourhood_id"),
                        rs.getString("event_id"),
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertMedia(Connection connection, Media m) throws SQLException {
        String sql = """
            INSERT INTO media (id, type, url, file_extension, neighbourhood_id, event_id, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                type=excluded.type, url=excluded.url, file_extension=excluded.file_extension, 
                neighbourhood_id=excluded.neighbourhood_id, event_id=excluded.event_id, 
                last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, m.id());
            pstmt.setString(2, m.type());
            pstmt.setString(3, m.url());
            pstmt.setString(4, m.fileExtension());
            pstmt.setString(5, m.neighbourhoodId());
            pstmt.setString(6, m.eventId());
            pstmt.setTimestamp(7, m.lastModified() != null ? Timestamp.valueOf(m.lastModified()) : null);
            pstmt.setString(8, m.syncStatus() != null ? m.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}