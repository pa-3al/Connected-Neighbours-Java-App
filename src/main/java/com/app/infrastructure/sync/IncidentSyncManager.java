package com.app.infrastructure.sync;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentPriority;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.IncidentService;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public class IncidentSyncManager {

    private final IncidentService incidentService;

    public IncidentSyncManager(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    public IncidentSyncReport sync(Path serverDatabasePath, Function<IncidentConflict, Incident> conflictResolver) {
        Objects.requireNonNull(serverDatabasePath, "serverDatabasePath must not be null");
        Objects.requireNonNull(conflictResolver, "conflictResolver must not be null");

        Map<String, Incident> localById = toMapById(incidentService.getAllIncidents());

        try (Connection serverConnection = openConnection(serverDatabasePath)) {
            ensureIncidentsTable(serverConnection);
            Map<String, Incident> serverById = loadServerIncidents(serverConnection);

            int pushedToServer = 0;
            int pulledFromServer = 0;
            int conflictsResolved = 0;
            int conflictsUnresolved = 0;
            int unchanged = 0;

            Set<String> allIds = new TreeSet<>();
            allIds.addAll(localById.keySet());
            allIds.addAll(serverById.keySet());

            for (String id : allIds) {
                Incident local = localById.get(id);
                Incident server = serverById.get(id);

                if (local == null && server != null) {
                    Incident synced = withSyncMetadata(server, LocalDateTime.now());
                    incidentService.updateIncident(synced);
                    pulledFromServer++;
                    continue;
                }

                if (local != null && server == null) {
                    Incident synced = withSyncMetadata(local, LocalDateTime.now());
                    incidentService.updateIncident(synced);
                    upsertIncident(serverConnection, synced);
                    pushedToServer++;
                    continue;
                }

                if (local == null) {
                    continue;
                }

                if (areEquivalent(local, server)) {
                    Incident synced = withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified()));
                    incidentService.updateIncident(synced);
                    unchanged++;
                    continue;
                }

                Incident resolved = conflictResolver.apply(new IncidentConflict(local, server));
                if (resolved == null) {
                    incidentService.updateIncident(local.withSyncStatus(SyncStatus.CONFLICT));
                    conflictsUnresolved++;
                    continue;
                }

                Incident synced = withSyncMetadata(resolved, LocalDateTime.now());
                incidentService.updateIncident(synced);
                upsertIncident(serverConnection, synced);
                conflictsResolved++;
            }

            return new IncidentSyncReport(
                pushedToServer,
                pulledFromServer,
                conflictsResolved,
                conflictsUnresolved,
                unchanged
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to synchronize incidents", e);
        }
    }

    private Map<String, Incident> toMapById(java.util.List<Incident> incidents) {
        Map<String, Incident> byId = new HashMap<>();
        for (Incident incident : incidents) {
            if (incident != null && incident.id() != null && !incident.id().isBlank()) {
                byId.put(incident.id(), incident);
            }
        }
        return byId;
    }

    private Connection openConnection(Path databasePath) throws SQLException {
        String pathStr = databasePath.toAbsolutePath().toString().replace('\\', '/');
        if (!pathStr.startsWith("/")) {
            pathStr = "/" + pathStr;
        }
        return DriverManager.getConnection("jdbc:sqlite:" + pathStr);
    }

    private void ensureIncidentsTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS incidents (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description CLOB,
                    category VARCHAR(50),
                    status VARCHAR(50),
                    priority VARCHAR(50),
                    reported_by VARCHAR(255),
                    location VARCHAR(255),
                    reported_at TIMESTAMP,
                    resolved_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);
        }
    }

    private Map<String, Incident> loadServerIncidents(Connection conn) throws SQLException {
        Map<String, Incident> incidents = new HashMap<>();
        String sql = "SELECT * FROM incidents";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Incident incident = mapRow(rs);
                incidents.put(incident.id(), incident);
            }
        }
        return incidents;
    }

    private void upsertIncident(Connection conn, Incident incident) throws SQLException {
        String sql = """
            INSERT INTO incidents (id, title, description, category, status, priority, reported_by, location, reported_at, resolved_at, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                category = excluded.category,
                status = excluded.status,
                priority = excluded.priority,
                reported_by = excluded.reported_by,
                location = excluded.location,
                reported_at = excluded.reported_at,
                resolved_at = excluded.resolved_at,
                last_modified = excluded.last_modified,
                sync_status = excluded.sync_status
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, incident.id());
            stmt.setString(2, incident.title());
            stmt.setString(3, incident.description());
            stmt.setString(4, incident.category() == null ? null : incident.category().name());
            stmt.setString(5, incident.status() == null ? null : incident.status().name());
            stmt.setString(6, incident.priority() == null ? null : incident.priority().name());
            stmt.setString(7, incident.reportedBy());
            stmt.setString(8, incident.location());
            stmt.setTimestamp(9, toTimestamp(incident.reportedAt()));
            stmt.setTimestamp(10, toTimestamp(incident.resolvedAt()));
            stmt.setTimestamp(11, toTimestamp(incident.lastModified()));
            stmt.setString(12, incident.syncStatus() == null ? null : incident.syncStatus().name());
            stmt.executeUpdate();
        }
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
            parseDbDate(rs.getString("reported_at")),
            parseDbDate(rs.getString("resolved_at")),
            parseDbDate(rs.getString("last_modified")),
            parseEnum(SyncStatus.class, rs.getString("sync_status"))
        );
    }

    private Incident withSyncMetadata(Incident incident, LocalDateTime syncTime) {
        return new Incident(
            incident.id(),
            incident.title(),
            incident.description(),
            incident.category(),
            incident.status(),
            incident.priority(),
            incident.reportedBy(),
            incident.location(),
            incident.reportedAt(),
            incident.resolvedAt(),
            syncTime,
            SyncStatus.SYNCED
        );
    }

    private LocalDateTime maxDate(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b == null ? LocalDateTime.now() : b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private boolean areEquivalent(Incident first, Incident second) {
        if (first == null || second == null) {
            return false;
        }
        return Objects.equals(first.id(), second.id())
            && Objects.equals(first.title(), second.title())
            && Objects.equals(first.description(), second.description())
            && Objects.equals(first.category(), second.category())
            && Objects.equals(first.status(), second.status())
            && Objects.equals(first.priority(), second.priority())
            && Objects.equals(first.reportedBy(), second.reportedBy())
            && Objects.equals(first.location(), second.location())
            && Objects.equals(first.reportedAt(), second.reportedAt())
            && Objects.equals(first.resolvedAt(), second.resolvedAt());
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
        } catch (Exception e) {
            System.err.println("Error parsing date in sync: " + dateStr + " - " + e.getMessage());
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
