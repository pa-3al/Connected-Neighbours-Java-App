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
        String sql = "SELECT * FROM incidents ORDER BY reported_at DESC";
        
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
        String sql = "SELECT * FROM incidents WHERE id = ?";
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
            MERGE INTO incidents (id, title, description, category, status, priority, reported_by, location, reported_at, resolved_at, last_modified, sync_status) 
            KEY(id) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, incident.id());
            stmt.setString(2, incident.title());
            stmt.setString(3, incident.description());
            stmt.setString(4, incident.category() != null ? incident.category().name() : null);
            stmt.setString(5, incident.status() != null ? incident.status().name() : null);
            stmt.setString(6, incident.priority() != null ? incident.priority().name() : null);
            stmt.setString(7, incident.reportedBy());
            stmt.setString(8, incident.location());
            stmt.setTimestamp(9, toTimestamp(incident.reportedAt()));
            stmt.setTimestamp(10, toTimestamp(incident.resolvedAt()));
            stmt.setTimestamp(11, toTimestamp(incident.lastModified()));
            stmt.setString(12, incident.syncStatus() != null ? incident.syncStatus().name() : null);
            
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
        String sql = "SELECT * FROM incidents WHERE status = ? ORDER BY reported_at DESC";
        
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
        return new Incident(
            rs.getString("id"),
            rs.getString("title"),
            rs.getString("description"),
            parseEnum(IncidentCategory.class, rs.getString("category")),
            parseEnum(IncidentStatus.class, rs.getString("status")),
            parseEnum(IncidentPriority.class, rs.getString("priority")),
            rs.getString("reported_by"),
            rs.getString("location"),
            toLocalDateTime(rs.getTimestamp("reported_at")),
            toLocalDateTime(rs.getTimestamp("resolved_at")),
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
