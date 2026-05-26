package com.app.infrastructure.sync;

import com.app.domain.model.Event;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EventSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public EventSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, Event> loadEventsById(Connection connection) throws SQLException {
        Map<String, Event> map = new HashMap<>();
        String sql = "SELECT * FROM events";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new Event(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getObject("points") != null ? rs.getInt("points") : null,
                        rs.getObject("real_money_price") != null ? rs.getDouble("real_money_price") : null,
                        rs.getBoolean("require_validation"),
                        rs.getString("signature_url"),
                        rs.getString("contract_id"),
                        rs.getString("address_id"),
                        rs.getString("created_by_user_id"),
                        rs.getString("approved_by_moderator_id"),
                        rs.getString("approved_by_admin_id"),
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertEvent(Connection connection, Event e) throws SQLException {
        String sql = """
            INSERT INTO events (id, name, description, points, real_money_price, require_validation, signature_url, contract_id, address_id, created_by_user_id, approved_by_moderator_id, approved_by_admin_id, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                name=excluded.name, description=excluded.description, points=excluded.points, 
                real_money_price=excluded.real_money_price, 
                require_validation=excluded.require_validation, signature_url=excluded.signature_url, 
                contract_id=excluded.contract_id, address_id=excluded.address_id, 
                created_by_user_id=excluded.created_by_user_id, approved_by_moderator_id=excluded.approved_by_moderator_id, 
                approved_by_admin_id=excluded.approved_by_admin_id, last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, e.id());
            pstmt.setString(2, e.name());
            pstmt.setString(3, e.description());
            if (e.points() != null) pstmt.setInt(4, e.points()); else pstmt.setNull(4, Types.INTEGER);
            if (e.realMoneyPrice() != null) pstmt.setDouble(5, e.realMoneyPrice()); else pstmt.setNull(5, Types.DOUBLE);
            pstmt.setBoolean(6, e.requireValidation() != null ? e.requireValidation() : false);
            pstmt.setString(7, e.signatureUrl());
            pstmt.setString(8, e.contractId());
            pstmt.setString(9, e.addressId());
            pstmt.setString(10, e.createdByUserId());
            pstmt.setString(11, e.approvedByModeratorId());
            pstmt.setString(12, e.approvedByAdminId());
            pstmt.setTimestamp(13, e.lastModified() != null ? Timestamp.valueOf(e.lastModified()) : null);
            pstmt.setString(14, e.syncStatus() != null ? e.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}
