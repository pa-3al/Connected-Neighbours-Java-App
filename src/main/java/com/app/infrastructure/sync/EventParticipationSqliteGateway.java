package com.app.infrastructure.sync;

import com.app.domain.model.EventParticipation;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EventParticipationSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public EventParticipationSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, EventParticipation> loadEventParticipationsById(Connection connection) throws SQLException {
        Map<String, EventParticipation> map = new HashMap<>();
        String sql = "SELECT * FROM event_participations";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new EventParticipation(
                        rs.getString("id"),
                        rs.getTimestamp("subscribed_at") != null ? rs.getTimestamp("subscribed_at").toLocalDateTime() : null,
                        rs.getString("status"),
                        rs.getString("signature_url"),
                        rs.getString("rejected_reason"),
                        rs.getString("user_id"),
                        rs.getString("event_id"),
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertEventParticipation(Connection connection, EventParticipation ep) throws SQLException {
        String sql = """
            INSERT INTO event_participations (id, subscribed_at, status, signature_url, rejected_reason, user_id, event_id, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                subscribed_at=excluded.subscribed_at, status=excluded.status, 
                signature_url=excluded.signature_url, rejected_reason=excluded.rejected_reason, 
                user_id=excluded.user_id, event_id=excluded.event_id, 
                last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ep.id());
            pstmt.setTimestamp(2, ep.subscribedAt() != null ? Timestamp.valueOf(ep.subscribedAt()) : null);
            pstmt.setString(3, ep.status());
            pstmt.setString(4, ep.signatureUrl());
            pstmt.setString(5, ep.rejectedReason());
            pstmt.setString(6, ep.userId());
            pstmt.setString(7, ep.eventId());
            pstmt.setTimestamp(8, ep.lastModified() != null ? Timestamp.valueOf(ep.lastModified()) : null);
            pstmt.setString(9, ep.syncStatus() != null ? ep.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}