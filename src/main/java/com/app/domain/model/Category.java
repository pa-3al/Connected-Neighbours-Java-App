package com.app.domain.model;

import java.time.LocalDateTime;

public record Category(
        String id,
        String name,
        String type,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String eventId,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public Category withSyncStatus(SyncStatus newStatus) {
        return new Category(id, name, type, active, createdAt, updatedAt, eventId, lastModified, newStatus);
    }
}