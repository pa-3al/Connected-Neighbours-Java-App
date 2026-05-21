package com.app.domain.service;

import com.app.domain.model.Service;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceService {

    private final DatabaseConfig databaseConfig;

    public ServiceService(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public List<Service> getAllServices() {
        List<Service> list = new ArrayList<>();
        String sql = "SELECT * FROM services";
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Service(
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
                        parseDbDate(rs.getString("created_at")),
                        parseDbDate(rs.getString("updated_at")),
                        parseDbDate(rs.getString("last_modified")),
                        rs.getString("sync_status") != null ? com.app.domain.model.SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public void updateService(Service s) {
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
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
            pstmt.setString(14, s.createdAt() != null ? s.createdAt().toString() : null);
            pstmt.setString(15, s.updatedAt() != null ? s.updatedAt().toString() : null);
            pstmt.setString(16, s.lastModified() != null ? s.lastModified().toString() : null);
            pstmt.setString(17, s.syncStatus() != null ? s.syncStatus().name() : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDateTime parseDbDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            if (dateStr.matches("^\\d+$")) {
                return LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(Long.parseLong(dateStr)),
                        java.time.ZoneId.systemDefault()
                );
            }
            String normalized = dateStr.replace(' ', 'T');
            return LocalDateTime.parse(normalized);
        } catch (Exception e) {
            return null;
        }
    }
}