package com.app.domain.model;

import java.time.LocalDateTime;

public record Event(
        String id,
        String name,
        String description,
        Integer points,
        Double realMoneyPrice,
        Boolean requireValidation,
        String signatureUrl,
        String contractId,
        String addressId,
        String createdByUserId,
        String approvedByModeratorId,
        String approvedByAdminId,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public Event withSyncStatus(SyncStatus newStatus) {
        return new Event(id, name, description, points, realMoneyPrice, requireValidation, signatureUrl, contractId, addressId, createdByUserId, approvedByModeratorId, approvedByAdminId, lastModified, newStatus);
    }
}