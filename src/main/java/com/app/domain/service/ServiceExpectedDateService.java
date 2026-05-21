package com.app.domain.service;

import com.app.domain.model.ServiceExpectedDate;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceExpectedDateService {

    private final DatabaseConfig databaseConfig;

    public ServiceExpectedDateService(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public List<ServiceExpectedDate> getAllServiceExpectedDates() {
        List<ServiceExpectedDate> list = new ArrayList<>();
        String sql = "SELECT * FROM service_expected_dates";
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ServiceExpectedDate(
                        rs.getString("id"),
                        rs.getTimestamp("start_date") != null ? rs.getTimestamp("start_date").toLocalDateTime() : null,
                        rs.getTimestamp("end_date") != null ? rs.getTimestamp("end_date").toLocalDateTime() : null,
                        rs.getString("service_id"),
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public void updateServiceExpectedDate(ServiceExpectedDate s) {
        String sql = """
            INSERT INTO service_expected_dates (id, start_date, end_date, service_id, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                start_date=excluded.start_date, end_date=excluded.end_date, 
                service_id=excluded.service_id, 
                last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.id());
            pstmt.setTimestamp(2, s.startDate() != null ? Timestamp.valueOf(s.startDate()) : null);
            pstmt.setTimestamp(3, s.endDate() != null ? Timestamp.valueOf(s.endDate()) : null);
            pstmt.setString(4, s.serviceId());
            pstmt.setTimestamp(5, s.lastModified() != null ? Timestamp.valueOf(s.lastModified()) : null);
            pstmt.setString(6, s.syncStatus() != null ? s.syncStatus().name() : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}