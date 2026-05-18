package com.app.domain.model;

import java.time.LocalDateTime;

public record EventParticipation(
        String id,
        LocalDateTime subscribedAt,
        String status,
        String signatureUrl,
        String rejectedReason,
        String userId,
        String eventId,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public EventParticipation withSyncStatus(SyncStatus newStatus) {
        return new EventParticipation(id, subscribedAt, status, signatureUrl, rejectedReason, userId, eventId, lastModified, newStatus);
    }
}