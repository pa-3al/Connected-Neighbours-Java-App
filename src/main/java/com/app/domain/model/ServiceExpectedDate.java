package com.app.domain.model;

import java.time.LocalDateTime;

public record ServiceExpectedDate(
        String id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String serviceId,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public ServiceExpectedDate withSyncStatus(SyncStatus newStatus) {
        return new ServiceExpectedDate(id, startDate, endDate, serviceId, lastModified, newStatus);
    }
}