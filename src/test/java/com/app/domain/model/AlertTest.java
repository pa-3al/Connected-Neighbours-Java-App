package com.app.domain.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AlertTest {

    @Test
    void createShouldBuildLocalOnlyActiveAlert() {
        LocalDateTime before = LocalDateTime.now();

        Alert alert = Alert.create("1", "Title", "Message", Alert.AlertSeverity.INFO, "admin", LocalDateTime.now().plusDays(1));

        LocalDateTime after = LocalDateTime.now();

        assertEquals("1", alert.id());
        assertEquals("Title", alert.title());
        assertEquals("Message", alert.message());
        assertEquals(Alert.AlertSeverity.INFO, alert.severity());
        assertTrue(alert.isActive());
        assertEquals("admin", alert.createdBy());
        assertEquals(SyncStatus.LOCAL_ONLY, alert.syncStatus());
        assertNotNull(alert.createdAt());
        assertNotNull(alert.lastModified());
        assertFalse(alert.createdAt().isBefore(before));
        assertFalse(alert.createdAt().isAfter(after));
    }

    @Test
    void withActiveShouldUpdateStatusAndMarkPending() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        LocalDateTime lastModified = LocalDateTime.now().minusHours(1);
        Alert alert = new Alert("1", "Title", "Message", Alert.AlertSeverity.WARNING, createdAt, null, true, "admin", lastModified, SyncStatus.SYNCED);

        Alert updated = alert.withActive(false);

        assertFalse(updated.isActive());
        assertEquals(SyncStatus.PENDING, updated.syncStatus());
        assertEquals(alert.id(), updated.id());
        assertEquals(alert.title(), updated.title());
        assertEquals(alert.createdAt(), updated.createdAt());
        assertEquals(alert.expiresAt(), updated.expiresAt());
        assertEquals(alert.createdBy(), updated.createdBy());
    }

    @Test
    void withSyncStatusShouldOnlyChangeSyncStatus() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        LocalDateTime lastModified = LocalDateTime.now().minusHours(1);
        Alert alert = new Alert("1", "Title", "Message", Alert.AlertSeverity.WARNING, createdAt, null, true, "admin", lastModified, SyncStatus.PENDING);

        Alert updated = alert.withSyncStatus(SyncStatus.SYNCED);

        assertEquals(SyncStatus.SYNCED, updated.syncStatus());
        assertEquals(alert.isActive(), updated.isActive());
        assertEquals(alert.lastModified(), updated.lastModified());
    }
}