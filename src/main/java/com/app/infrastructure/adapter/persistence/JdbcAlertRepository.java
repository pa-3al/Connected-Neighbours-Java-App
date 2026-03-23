package com.app.infrastructure.adapter.persistence;

import com.app.domain.model.Alert;
import com.app.domain.model.Alert.AlertSeverity;
import com.app.domain.model.SyncStatus;
import com.app.domain.port.out.AlertRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAlertRepository implements AlertRepository {

    private final DatabaseConfig databaseConfig;

    public JdbcAlertRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public List<Alert> findAll() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts ORDER BY created_at DESC";
        
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching alerts", e);
        }
        return alerts;
    }

    @Override
    public List<Alert> findActive() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE is_active = TRUE ORDER BY created_at DESC";
        
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching active alerts", e);
        }
        return alerts;
    }

    @Override
    public Optional<Alert> findById(String id) {
        String sql = "SELECT * FROM alerts WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding alert by id", e);
        }
        return Optional.empty();
    }

    @Override
    public Alert save(Alert alert) {
         String sql = """
            INSERT INTO alerts (id, title, message, severity, created_at, expires_at, is_active, created_by, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                message = excluded.message,
                severity = excluded.severity,
                created_at = excluded.created_at,
                expires_at = excluded.expires_at,
                is_active = excluded.is_active,
                created_by = excluded.created_by,
                last_modified = excluded.last_modified,
                sync_status = excluded.sync_status
        """;
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, alert.id());
            stmt.setString(2, alert.title());
            stmt.setString(3, alert.message());
            stmt.setString(4, alert.severity() != null ? alert.severity().name() : null);
            stmt.setTimestamp(5, toTimestamp(alert.createdAt()));
            stmt.setTimestamp(6, toTimestamp(alert.expiresAt()));
            stmt.setBoolean(7, alert.isActive());
            stmt.setString(8, alert.createdBy());
            stmt.setTimestamp(9, toTimestamp(alert.lastModified()));
            stmt.setString(10, alert.syncStatus() != null ? alert.syncStatus().name() : null);
            
            stmt.executeUpdate();
            return alert;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error saving alert", e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM alerts WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting alert", e);
        }
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        return new Alert(
            rs.getString("id"),
            rs.getString("title"),
            rs.getString("message"),
            parseEnum(AlertSeverity.class, rs.getString("severity")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("expires_at")),
            rs.getBoolean("is_active"),
            rs.getString("created_by"),
            toLocalDateTime(rs.getTimestamp("last_modified")),
            parseEnum(SyncStatus.class, rs.getString("sync_status"))
        );
    }

    private Timestamp toTimestamp(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }
    
    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
    
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null; 
        }
    }
}
