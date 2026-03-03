package com.app.domain.model;

import java.time.LocalDateTime;

public record Alert(
    String id,
    String title,
    String message,
    AlertSeverity severity,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    boolean isActive,
    String createdBy,
    LocalDateTime lastModified,
    SyncStatus syncStatus
) {
    public enum AlertSeverity {
        INFO, WARNING, DANGER, CRITICAL
    }

    public Alert withActive(boolean active) {
        return new Alert(id, title, message, severity, createdAt, expiresAt,
            active, createdBy, LocalDateTime.now(), SyncStatus.PENDING);
    }

    public Alert withSyncStatus(SyncStatus newSyncStatus) {
        return new Alert(id, title, message, severity, createdAt, expiresAt,
            isActive, createdBy, lastModified, newSyncStatus);
    }

    public static Alert create(String id, String title, String message,
            AlertSeverity severity, String createdBy, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return new Alert(id, title, message, severity, now, expiresAt,
            true, createdBy, now, SyncStatus.LOCAL_ONLY);
    }
}
