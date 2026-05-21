package com.app.infrastructure.sync;

import com.app.domain.model.Service;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public ServiceSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, Service> loadServicesById(Connection connection) throws SQLException {
        Map<String, Service> map = new HashMap<>();
        String sql = "SELECT * FROM services";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new Service(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("points"),
                        rs.getString("status"),
                        rs.getString("moderator_comment"),
                        rs.getString("signature_url"),
                        rs.getString("contract_id"),
                        rs.getString("service_type_id"),
                        rs.getString("address_id"),
                        rs.getString("created_by_user_id"),
                        rs.getString("approved_by_moderator_id"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertService(Connection connection, Service s) throws SQLException {
        String sql = """
            INSERT INTO services (id, type, title, description, points, status, moderator_comment, signature_url, contract_id, service_type_id, address_id, created_by_user_id, approved_by_moderator_id, created_at, updated_at, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                type=excluded.type, title=excluded.title, description=excluded.description, 
                points=excluded.points, status=excluded.status, moderator_comment=excluded.moderator_comment, 
                signature_url=excluded.signature_url, contract_id=excluded.contract_id, 
                service_type_id=excluded.service_type_id, address_id=excluded.address_id, 
                created_by_user_id=excluded.created_by_user_id, approved_by_moderator_id=excluded.approved_by_moderator_id, 
                created_at=excluded.created_at, updated_at=excluded.updated_at, 
                last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, s.id());
            pstmt.setString(2, s.type());
            pstmt.setString(3, s.title());
            pstmt.setString(4, s.description());
            if (s.points() != null) {
                pstmt.setInt(5, s.points());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            pstmt.setString(6, s.status());
            pstmt.setString(7, s.moderatorComment());
            pstmt.setString(8, s.signatureUrl());
            pstmt.setString(9, s.contractId());
            pstmt.setString(10, s.serviceTypeId());
            pstmt.setString(11, s.addressId());
            pstmt.setString(12, s.createdByUserId());
            pstmt.setString(13, s.approvedByModeratorId());
            pstmt.setTimestamp(14, s.createdAt() != null ? Timestamp.valueOf(s.createdAt()) : null);
            pstmt.setTimestamp(15, s.updatedAt() != null ? Timestamp.valueOf(s.updatedAt()) : null);
            pstmt.setTimestamp(16, s.lastModified() != null ? Timestamp.valueOf(s.lastModified()) : null);
            pstmt.setString(17, s.syncStatus() != null ? s.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}