package com.app.domain.model;

import java.time.LocalDateTime;

public record Neighbourhood(
        String id,
        String name,
        String description,
        String city,
        String postalCode,
        String countryCode,
        Integer estimatedPopulation,
        String polygon,
        Integer area,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public Neighbourhood withSyncStatus(SyncStatus newStatus) {
        return new Neighbourhood(id, name, description, city, postalCode, countryCode, estimatedPopulation, polygon, area, lastModified, newStatus);
    }
}