package com.app.domain.model;

import java.time.LocalDateTime;

public record Incident(
    String id,
    String title,
    String description,
    IncidentCategory category,
    IncidentStatus status,
    IncidentPriority priority,
    String reportedByUserId,
    String reportedBy,
    String location,
    LocalDateTime reportedAt,
    LocalDateTime resolvedAt,
    LocalDateTime lastModified,
    SyncStatus syncStatus
) {
    public enum IncidentCategory {
        NOISE, SECURITY, CLEANLINESS, INFRASTRUCTURE, OTHER
    }

    public enum IncidentStatus {
        OPEN, IN_PROGRESS, RESOLVED, CLOSED
    }

    public enum IncidentPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public Incident withStatus(IncidentStatus newStatus) {
        LocalDateTime resolved = (newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED)
            ? LocalDateTime.now() : this.resolvedAt;
        return new Incident(id, title, description, category, newStatus, priority,
            reportedByUserId, reportedBy, location, reportedAt, resolved, LocalDateTime.now(), SyncStatus.PENDING);
    }

    public Incident withSyncStatus(SyncStatus newSyncStatus) {
        return new Incident(id, title, description, category, status, priority,
            reportedByUserId, reportedBy, location, reportedAt, resolvedAt, lastModified, newSyncStatus);
    }

    public static Incident create(String id, String title, String description,
            IncidentCategory category, IncidentPriority priority,
            String reportedBy, String location) {
        LocalDateTime now = LocalDateTime.now();
        return new Incident(id, title, description, category, IncidentStatus.OPEN,
            priority, null, reportedBy, location, now, null, now, SyncStatus.LOCAL_ONLY);
    }
}
