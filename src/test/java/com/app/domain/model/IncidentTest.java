package com.app.domain.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class IncidentTest {

    @Test
    void createShouldBuildPendingLocalOnlyIncident() {
        LocalDateTime before = LocalDateTime.now();

        Incident incident = Incident.create(
                "inc-1",
                "Panne ascenseur",
                "Ascenseur bloque",
                Incident.IncidentCategory.SERVICE,
                "user-1"
        );

        LocalDateTime after = LocalDateTime.now();

        assertEquals("inc-1", incident.id());
        assertEquals("Panne ascenseur", incident.title());
        assertEquals("Ascenseur bloque", incident.description());
        assertEquals(Incident.IncidentCategory.SERVICE, incident.category());
        assertEquals(Incident.IncidentStatus.PENDING, incident.status());
        assertEquals("user-1", incident.reportedByUserId());
        assertNull(incident.reportedBy());
        assertNull(incident.adminResponseMessage());
        assertNull(incident.resolvedAt());
        assertEquals(SyncStatus.LOCAL_ONLY, incident.syncStatus());
        assertNotNull(incident.reportedAt());
        assertNotNull(incident.lastModified());
        assertFalse(incident.reportedAt().isBefore(before));
        assertFalse(incident.reportedAt().isAfter(after));
        assertFalse(incident.lastModified().isBefore(before));
        assertFalse(incident.lastModified().isAfter(after));
    }

    @Test
    void withStatusShouldSetResolvedAtWhenCompletedAndMarkPendingSync() {
        LocalDateTime reportedAt = LocalDateTime.now().minusDays(2);
        LocalDateTime oldLastModified = LocalDateTime.now().minusHours(1);
        Incident incident = new Incident(
                "inc-2",
                "Titre",
                "Description",
                Incident.IncidentCategory.EVENT,
                Incident.IncidentStatus.PENDING,
                "user-2",
                "Bob",
                "En cours",
                reportedAt,
                null,
                oldLastModified,
                SyncStatus.SYNCED
        );

        LocalDateTime before = LocalDateTime.now();
        Incident updated = incident.withStatus(Incident.IncidentStatus.COMPLETED);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(Incident.IncidentStatus.COMPLETED, updated.status());
        assertEquals(SyncStatus.PENDING, updated.syncStatus());
        assertNotNull(updated.resolvedAt());
        assertFalse(updated.resolvedAt().isBefore(before));
        assertFalse(updated.resolvedAt().isAfter(after));
        assertFalse(updated.lastModified().isBefore(before));
        assertFalse(updated.lastModified().isAfter(after));
        assertEquals(incident.id(), updated.id());
        assertEquals(incident.title(), updated.title());
        assertEquals(incident.reportedAt(), updated.reportedAt());
    }

    @Test
    void withStatusShouldKeepResolvedAtWhenNewStatusIsNotCompleted() {
        LocalDateTime resolvedAt = LocalDateTime.now().minusHours(2);
        Incident incident = new Incident(
                "inc-3",
                "Titre",
                "Description",
                Incident.IncidentCategory.OTHER,
                Incident.IncidentStatus.COMPLETED,
                "user-3",
                "Alice",
                null,
                LocalDateTime.now().minusDays(1),
                resolvedAt,
                LocalDateTime.now().minusHours(3),
                SyncStatus.SYNCED
        );

        Incident updated = incident.withStatus(Incident.IncidentStatus.PENDING);

        assertEquals(Incident.IncidentStatus.PENDING, updated.status());
        assertEquals(resolvedAt, updated.resolvedAt());
        assertEquals(SyncStatus.PENDING, updated.syncStatus());
    }

    @Test
    void withSyncStatusShouldOnlyChangeSyncStatus() {
        Incident incident = new Incident(
                "inc-4",
                "Titre",
                "Description",
                Incident.IncidentCategory.SERVICE,
                Incident.IncidentStatus.PENDING,
                "user-4",
                "Charlie",
                "-",
                LocalDateTime.now().minusDays(1),
                null,
                LocalDateTime.now().minusHours(1),
                SyncStatus.LOCAL_ONLY
        );

        Incident updated = incident.withSyncStatus(SyncStatus.CONFLICT);

        assertEquals(SyncStatus.CONFLICT, updated.syncStatus());
        assertEquals(incident.status(), updated.status());
        assertEquals(incident.lastModified(), updated.lastModified());
        assertEquals(incident.resolvedAt(), updated.resolvedAt());
        assertEquals(incident.reportedByUserId(), updated.reportedByUserId());
    }
}