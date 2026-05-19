package com.app.infrastructure.sync;

import com.app.domain.model.EventPlanning;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EventPlanningSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public EventPlanningSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, EventPlanning> loadEventPlanningsById(Connection connection) throws SQLException {
        Map<String, EventPlanning> map = new HashMap<>();
        String sql = "SELECT * FROM event_plannings";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new EventPlanning(
                        rs.getString("id"),
                        rs.getTimestamp("start_date") != null ? rs.getTimestamp("start_date").toLocalDateTime() : null,
                        rs.getTimestamp("end_date") != null ? rs.getTimestamp("end_date").toLocalDateTime() : null,
                        rs.getString("event_id"),
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertEventPlanning(Connection connection, EventPlanning ep) throws SQLException {
        String sql = """
            INSERT INTO event_plannings (id, start_date, end_date, event_id, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                start_date=excluded.start_date, end_date=excluded.end_date, 
                event_id=excluded.event_id, last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ep.id());
            pstmt.setTimestamp(2, ep.startDate() != null ? Timestamp.valueOf(ep.startDate()) : null);
            pstmt.setTimestamp(3, ep.endDate() != null ? Timestamp.valueOf(ep.endDate()) : null);
            pstmt.setString(4, ep.eventId());
            pstmt.setTimestamp(5, ep.lastModified() != null ? Timestamp.valueOf(ep.lastModified()) : null);
            pstmt.setString(6, ep.syncStatus() != null ? ep.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}