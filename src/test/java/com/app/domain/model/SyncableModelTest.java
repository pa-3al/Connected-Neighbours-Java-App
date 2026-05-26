package com.app.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class SyncableModelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 26, 10, 15, 30);

    @Test
    void addressWithSyncStatusShouldOnlyChangeSyncStatus() {
        Address address = new Address("address-1", "12", "Apt 4", "Main street", "Paris", "75001", "IDF", "FR", "POINT(1 2)", true, "neighbourhood-1", "user-1", NOW, SyncStatus.LOCAL_ONLY);

        Address updated = address.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(address, updated);
        assertEquals(new Address("address-1", "12", "Apt 4", "Main street", "Paris", "75001", "IDF", "FR", "POINT(1 2)", true, "neighbourhood-1", "user-1", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void categoryWithSyncStatusShouldOnlyChangeSyncStatus() {
        Category category = new Category("category-1", "Repair", "SERVICE", true, NOW.minusDays(2), NOW.minusDays(1), "event-1", NOW, SyncStatus.LOCAL_ONLY);

        Category updated = category.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(category, updated);
        assertEquals(new Category("category-1", "Repair", "SERVICE", true, NOW.minusDays(2), NOW.minusDays(1), "event-1", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void contractTemplateWithSyncStatusShouldOnlyChangeSyncStatus() {
        ContractTemplate template = new ContractTemplate("contract-1", "SERVICE", "fr", "/contracts/service.pdf", "service.pdf", "pdf", true, NOW.minusDays(2), NOW.minusDays(1), NOW, SyncStatus.LOCAL_ONLY);

        ContractTemplate updated = template.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(template, updated);
        assertEquals(new ContractTemplate("contract-1", "SERVICE", "fr", "/contracts/service.pdf", "service.pdf", "pdf", true, NOW.minusDays(2), NOW.minusDays(1), NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void eventWithSyncStatusShouldOnlyChangeSyncStatus() {
        Event event = new Event("event-1", "Garden day", "Planting", 20, 3.5, true, "/signatures/e1.png", "contract-1", "address-1", "user-1", "moderator-1", "admin-1", NOW, SyncStatus.LOCAL_ONLY);

        Event updated = event.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(event, updated);
        assertEquals(new Event("event-1", "Garden day", "Planting", 20, 3.5, true, "/signatures/e1.png", "contract-1", "address-1", "user-1", "moderator-1", "admin-1", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void eventParticipationWithSyncStatusShouldOnlyChangeSyncStatus() {
        EventParticipation participation = new EventParticipation("participation-1", NOW.minusHours(2), "VALIDATED", "/signatures/p1.png", "none", "user-1", "event-1", NOW, SyncStatus.LOCAL_ONLY);

        EventParticipation updated = participation.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(participation, updated);
        assertEquals(new EventParticipation("participation-1", NOW.minusHours(2), "VALIDATED", "/signatures/p1.png", "none", "user-1", "event-1", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void eventPlanningWithSyncStatusShouldOnlyChangeSyncStatus() {
        EventPlanning planning = new EventPlanning("planning-1", NOW.plusDays(1), NOW.plusDays(2), "event-1", NOW, SyncStatus.LOCAL_ONLY);

        EventPlanning updated = planning.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(planning, updated);
        assertEquals(new EventPlanning("planning-1", NOW.plusDays(1), NOW.plusDays(2), "event-1", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void eventTagWithSyncStatusShouldOnlyChangeSyncStatus() {
        EventTag tag = new EventTag("garden", NOW, SyncStatus.LOCAL_ONLY);

        EventTag updated = tag.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(tag, updated);
        assertEquals(new EventTag("garden", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void mediaWithSyncStatusShouldOnlyChangeSyncStatus() {
        Media media = new Media("media-1", "image", "/media/photo.png", "png", "neighbourhood-1", "event-1", NOW, SyncStatus.LOCAL_ONLY);

        Media updated = media.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(media, updated);
        assertEquals(new Media("media-1", "image", "/media/photo.png", "png", "neighbourhood-1", "event-1", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void neighbourhoodWithSyncStatusShouldOnlyChangeSyncStatus() {
        Neighbourhood neighbourhood = new Neighbourhood("neighbourhood-1", "Centre", "Downtown", "Paris", "75001", "FR", 1200, "POLYGON((0 0,1 1))", 42, NOW, SyncStatus.LOCAL_ONLY);

        Neighbourhood updated = neighbourhood.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(neighbourhood, updated);
        assertEquals(new Neighbourhood("neighbourhood-1", "Centre", "Downtown", "Paris", "75001", "FR", 1200, "POLYGON((0 0,1 1))", 42, NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void serviceExpectedDateWithSyncStatusShouldOnlyChangeSyncStatus() {
        ServiceExpectedDate date = new ServiceExpectedDate("date-1", NOW.plusDays(1), NOW.plusDays(2), "service-1", NOW, SyncStatus.LOCAL_ONLY);

        ServiceExpectedDate updated = date.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(date, updated);
        assertEquals(new ServiceExpectedDate("date-1", NOW.plusDays(1), NOW.plusDays(2), "service-1", NOW, SyncStatus.SYNCED), updated);
    }

    @Test
    void serviceWithSyncStatusShouldOnlyChangeSyncStatus() {
        Service service = new Service("service-1", "HELP", "Carry groceries", "Help a neighbour", 8, "PENDING", "ok", "/signatures/s1.png", "contract-1", "category-1", "address-1", "user-1", "moderator-1", NOW.minusDays(2), NOW.minusDays(1), NOW, SyncStatus.LOCAL_ONLY);

        Service updated = service.withSyncStatus(SyncStatus.SYNCED);

        assertNotSame(service, updated);
        assertEquals(new Service("service-1", "HELP", "Carry groceries", "Help a neighbour", 8, "PENDING", "ok", "/signatures/s1.png", "contract-1", "category-1", "address-1", "user-1", "moderator-1", NOW.minusDays(2), NOW.minusDays(1), NOW, SyncStatus.SYNCED), updated);
    }
}
