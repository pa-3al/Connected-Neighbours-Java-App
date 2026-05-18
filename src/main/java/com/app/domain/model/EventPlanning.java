package com.app.domain.model;

import java.time.LocalDateTime;

public record EventPlanning(
        String id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String eventId,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public EventPlanning withSyncStatus(SyncStatus newStatus) {
        return new EventPlanning(id, startDate, endDate, eventId, lastModified, newStatus);
    }
}