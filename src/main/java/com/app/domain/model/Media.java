package com.app.domain.model;

import java.time.LocalDateTime;

public record Media(
        String id,
        String type,
        String url,
        String fileExtension,
        String neighbourhoodId,
        String eventId,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public Media withSyncStatus(SyncStatus newStatus) {
        return new Media(id, type, url, fileExtension, neighbourhoodId, eventId, lastModified, newStatus);
    }
}