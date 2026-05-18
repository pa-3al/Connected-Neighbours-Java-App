package com.app.domain.model;

import java.time.LocalDateTime;

public record EventTag(
        String name,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public EventTag withSyncStatus(SyncStatus newStatus) {
        return new EventTag(name, lastModified, newStatus);
    }
}