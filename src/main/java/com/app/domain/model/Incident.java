package com.app.domain.model;

import java.time.LocalDateTime;

public record Incident(
        String id,
        String title,
        String description,
        IncidentCategory category,
        IncidentStatus status,
        String reportedByUserId,
        String reportedBy,
        String adminResponseMessage,
        LocalDateTime reportedAt,
        LocalDateTime resolvedAt,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public enum IncidentCategory {
        SERVICE, EVENT, OTHER
    }

    public enum IncidentStatus {
        PENDING, COMPLETED
    }

    public Incident withStatus(IncidentStatus newStatus) {
        LocalDateTime resolved = (newStatus == IncidentStatus.COMPLETED)
                ? LocalDateTime.now() : this.resolvedAt;
        return new Incident(id, title, description, category, newStatus,
                reportedByUserId, reportedBy, adminResponseMessage, reportedAt, resolved, LocalDateTime.now(), SyncStatus.PENDING);
    }

    public Incident withSyncStatus(SyncStatus newSyncStatus) {
        return new Incident(id, title, description, category, status,
                reportedByUserId, reportedBy, adminResponseMessage, reportedAt, resolvedAt, lastModified, newSyncStatus);
    }

    public static Incident create(String id, String title, String description,
                                  IncidentCategory category, String reportedByUserId) {
        LocalDateTime now = LocalDateTime.now();
        return new Incident(id, title, description, category, IncidentStatus.PENDING,
                reportedByUserId, null, null, now, null, now, SyncStatus.LOCAL_ONLY);
    }
}