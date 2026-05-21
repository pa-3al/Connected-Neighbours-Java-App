package com.app.domain.model;

import java.time.LocalDateTime;

public record Service(
        String id,
        String type,
        String title,
        String description,
        Integer points,
        String status,
        String moderatorComment,
        String signatureUrl,
        String contractId,
        String serviceTypeId,
        String addressId,
        String createdByUserId,
        String approvedByModeratorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public Service withSyncStatus(SyncStatus newStatus) {
        return new Service(id, type, title, description, points, status, moderatorComment, signatureUrl, contractId, serviceTypeId, addressId, createdByUserId, approvedByModeratorId, createdAt, updatedAt, lastModified, newStatus);
    }
}