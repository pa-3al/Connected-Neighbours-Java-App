package com.app.domain.model;

import java.time.LocalDateTime;

public record ContractTemplate(
        String id,
        String contractType,
        String languageId,
        String documentPath,
        String originalFileName,
        String fileExtension,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public ContractTemplate withSyncStatus(SyncStatus newStatus) {
        return new ContractTemplate(id, contractType, languageId, documentPath, originalFileName, fileExtension, active, createdAt, updatedAt, lastModified, newStatus);
    }
}