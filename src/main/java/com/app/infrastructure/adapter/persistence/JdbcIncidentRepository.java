package com.app.infrastructure.adapter.persistence;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentPriority;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.model.SyncStatus;
import com.app.domain.port.out.IncidentRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcIncidentRepository implements IncidentRepository {

    private final DatabaseConfig databaseConfig;

    public JdbcIncidentRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public List<Incident> findAll() {
        List<Incident> incidents = new ArrayList<>();
        String sql = """
            SELECT i.*, COALESCE(NULLIF(TRIM(u.firstname || ' ' || u.lastname), ''), i.reported_by) AS reported_by_display
            FROM incidents i
            LEFT JOIN users u ON i.reported_by_user_id = u.id
            ORDER BY i.reported_at DESC
            """;
        
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                incidents.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching incidents", e);
        }
        return incidents;
    }

    @Override
    public Optional<Incident> findById(String id) {
        String sql = """
            SELECT i.*, COALESCE(NULLIF(TRIM(u.firstname || ' ' || u.lastname), ''), i.reported_by) AS reported_by_display
            FROM incidents i
            LEFT JOIN users u ON i.reported_by_user_id = u.id
            WHERE i.id = ?
            """;
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding incident by id", e);
        }
        return Optional.empty();
    }

    @Override
    public Incident save(Incident incident) {
        String sql = """
            INSERT INTO incidents (id, title, description, category, status, priority, reported_by_user_id, reported_by, location, reported_at, resolved_at, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                category = excluded.category,
                status = excluded.status,
                priority = excluded.priority,
                reported_by_user_id = excluded.reported_by_user_id,
                reported_by = excluded.reported_by,
                location = excluded.location,
                reported_at = excluded.reported_at,
                resolved_at = excluded.resolved_at,
                last_modified = excluded.last_modified,
                sync_status = excluded.sync_status
        """;
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, incident.id());
            stmt.setString(2, incident.title());
            stmt.setString(3, incident.description());
            stmt.setString(4, incident.category() != null ? incident.category().name() : null);
            stmt.setString(5, incident.status() != null ? incident.status().name() : null);
            stmt.setString(6, incident.priority() != null ? incident.priority().name() : null);
            stmt.setString(7, incident.reportedByUserId());
            stmt.setString(8, incident.reportedBy());
            stmt.setString(9, incident.location());
            stmt.setTimestamp(10, toTimestamp(incident.reportedAt()));
            stmt.setTimestamp(11, toTimestamp(incident.resolvedAt()));
            stmt.setTimestamp(12, toTimestamp(incident.lastModified()));
            stmt.setString(13, incident.syncStatus() != null ? incident.syncStatus().name() : null);
            
            stmt.executeUpdate();
            return incident;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error saving incident", e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM incidents WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting incident", e);
        }
    }

    @Override
    public List<Incident> findByStatus(IncidentStatus status) {
        List<Incident> incidents = new ArrayList<>();
        String sql = """
            SELECT i.*, COALESCE(NULLIF(TRIM(u.firstname || ' ' || u.lastname), ''), i.reported_by) AS reported_by_display
            FROM incidents i
            LEFT JOIN users u ON i.reported_by_user_id = u.id
            WHERE i.status = ?
            ORDER BY i.reported_at DESC
            """;
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    incidents.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching incidents by status", e);
        }
        return incidents;
    }

    private Incident mapRow(ResultSet rs) throws SQLException {
        String reportedByDisplay = rs.getString("reported_by_display");
        if (reportedByDisplay == null || reportedByDisplay.isBlank()) {
            reportedByDisplay = rs.getString("reported_by");
        }

        return new Incident(
            rs.getString("id"),
            rs.getString("title"),
            rs.getString("description"),
            parseEnum(IncidentCategory.class, rs.getString("category")),
            parseEnum(IncidentStatus.class, rs.getString("status")),
            parseEnum(IncidentPriority.class, rs.getString("priority")),
            rs.getString("reported_by_user_id"),
            reportedByDisplay,
            rs.getString("location"),
            parseDbDate(rs.getString("reported_at")),
            parseDbDate(rs.getString("resolved_at")),
            parseDbDate(rs.getString("last_modified")),
            parseEnum(SyncStatus.class, rs.getString("sync_status"))
        );
    }

    private Timestamp toTimestamp(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
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
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            System.err.println("Error parsing date: " + dateStr + " - " + e.getMessage());
            return null;
        }
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
