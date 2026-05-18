package com.app.domain.model;

import java.time.LocalDateTime;

public record Address(
        String id,
        String streetNumber,
        String addressLine2,
        String streetName,
        String city,
        String postalCode,
        String region,
        String countryCode,
        String location,
        Boolean active,
        String neighbourhoodId,
        String userId,
        LocalDateTime lastModified,
        SyncStatus syncStatus
) {
    public Address withSyncStatus(SyncStatus newStatus) {
        return new Address(id, streetNumber, addressLine2, streetName, city, postalCode, region, countryCode, location, active, neighbourhoodId, userId, lastModified, newStatus);
    }
}