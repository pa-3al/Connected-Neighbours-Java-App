package com.app.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.app.domain.model.Address;
import com.app.domain.model.Category;
import com.app.domain.model.ContractTemplate;
import com.app.domain.model.Event;
import com.app.domain.model.EventParticipation;
import com.app.domain.model.EventPlanning;
import com.app.domain.model.EventTag;
import com.app.domain.model.Media;
import com.app.domain.model.Neighbourhood;
import com.app.domain.model.Service;
import com.app.domain.model.ServiceExpectedDate;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.adapter.persistence.SchemaInitializer;

class DatabaseBackedServicesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 26, 10, 30);

    @TempDir
    Path tempDir;

    @Test
    void addressServiceShouldPersistAndLoadAddresses() {
        DatabaseConfig config = initializedDatabase("addresses.sqlite");
        AddressService service = new AddressService(config);
        Address address = new Address("address-1", "12", "Apt 4", "Main street", "Paris", "75001", "IDF", "FR", "POINT(1 2)", true, "neighbourhood-1", "user-1", NOW, SyncStatus.LOCAL_ONLY);

        service.updateAddress(address);

        assertEquals(List.of(address), service.getAllAddresses());
    }

    @Test
    void categoryServiceShouldPersistAndLoadCategories() {
        DatabaseConfig config = initializedDatabase("categories.sqlite");
        CategoryService service = new CategoryService(config);
        Category category = new Category("category-1", "Repair", "SERVICE", true, NOW.minusDays(2), NOW.minusDays(1), "event-1", NOW, SyncStatus.LOCAL_ONLY);

        service.updateCategory(category);

        assertEquals(List.of(category), service.getAllCategories());
    }

    @Test
    void contractTemplateServiceShouldPersistAndLoadContractTemplates() {
        DatabaseConfig config = initializedDatabase("contract-templates.sqlite");
        ContractTemplateService service = new ContractTemplateService(config);
        ContractTemplate template = new ContractTemplate("contract-1", "SERVICE", "fr", "/contracts/service.pdf", "service.pdf", "pdf", true, NOW.minusDays(2), NOW.minusDays(1), NOW, SyncStatus.LOCAL_ONLY);

        service.updateContractTemplate(template);

        assertEquals(List.of(template), service.getAllContractTemplates());
    }

    @Test
    void eventParticipationServiceShouldPersistAndLoadEventParticipations() {
        DatabaseConfig config = initializedDatabase("event-participations.sqlite");
        EventParticipationService service = new EventParticipationService(config);
        EventParticipation participation = new EventParticipation("participation-1", NOW.minusHours(2), "VALIDATED", "/signatures/p1.png", "none", "user-1", "event-1", NOW, SyncStatus.LOCAL_ONLY);

        service.updateEventParticipation(participation);

        assertEquals(List.of(participation), service.getAllEventParticipations());
    }

    @Test
    void eventPlanningServiceShouldPersistAndLoadEventPlannings() {
        DatabaseConfig config = initializedDatabase("event-plannings.sqlite");
        EventPlanningService service = new EventPlanningService(config);
        EventPlanning planning = new EventPlanning("planning-1", NOW.plusDays(1), NOW.plusDays(2), "event-1", NOW, SyncStatus.LOCAL_ONLY);

        service.updateEventPlanning(planning);

        assertEquals(List.of(planning), service.getAllEventPlannings());
    }

    @Test
    void eventServiceShouldPersistAndLoadEvents() {
        DatabaseConfig config = initializedDatabase("events.sqlite");
        EventService service = new EventService(config);
        Event event = new Event("event-1", "Garden day", "Planting", 20, 3.5, true, "/signatures/e1.png", "contract-1", "address-1", "user-1", "moderator-1", "admin-1", NOW, SyncStatus.LOCAL_ONLY);

        service.updateEvent(event);

        assertEquals(List.of(event), service.getAllEvents());
    }

    @Test
    void eventTagServiceShouldPersistAndLoadEventTags() {
        DatabaseConfig config = initializedDatabase("event-tags.sqlite");
        EventTagService service = new EventTagService(config);
        EventTag tag = new EventTag("garden", NOW, SyncStatus.LOCAL_ONLY);

        service.updateEventTag(tag);

        assertEquals(List.of(tag), service.getAllEventTags());
    }

    @Test
    void mediaServiceShouldPersistAndLoadMedia() {
        DatabaseConfig config = initializedDatabase("media.sqlite");
        MediaService service = new MediaService(config);
        Media media = new Media("media-1", "image", "/media/photo.png", "png", "neighbourhood-1", "event-1", NOW, SyncStatus.LOCAL_ONLY);

        service.updateMedia(media);

        assertEquals(List.of(media), service.getAllMedia());
    }

    @Test
    void neighbourhoodServiceShouldPersistAndLoadNeighbourhoods() {
        DatabaseConfig config = initializedDatabase("neighbourhoods.sqlite");
        NeighbourhoodService service = new NeighbourhoodService(config);
        Neighbourhood neighbourhood = new Neighbourhood("neighbourhood-1", "Centre", "Downtown", "Paris", "75001", "FR", 1200, "POLYGON((0 0,1 1))", 42, NOW, SyncStatus.LOCAL_ONLY);

        service.updateNeighbourhood(neighbourhood);

        assertEquals(List.of(neighbourhood), service.getAllNeighbourhoods());
    }

    @Test
    void serviceExpectedDateServiceShouldPersistAndLoadServiceExpectedDates() {
        DatabaseConfig config = initializedDatabase("service-expected-dates.sqlite");
        ServiceExpectedDateService service = new ServiceExpectedDateService(config);
        ServiceExpectedDate date = new ServiceExpectedDate("date-1", NOW.plusDays(1), NOW.plusDays(2), "service-1", NOW, SyncStatus.LOCAL_ONLY);

        service.updateServiceExpectedDate(date);

        assertEquals(List.of(date), service.getAllServiceExpectedDates());
    }

    @Test
    void serviceServiceShouldPersistAndLoadServices() {
        DatabaseConfig config = initializedDatabase("services.sqlite");
        ServiceService service = new ServiceService(config);
        Service neighbourService = new Service("service-1", "HELP", "Carry groceries", "Help a neighbour", 8, "PENDING", "ok", "/signatures/s1.png", "contract-1", "category-1", "address-1", "user-1", "moderator-1", NOW.minusDays(2), NOW.minusDays(1), NOW, SyncStatus.LOCAL_ONLY);

        service.updateService(neighbourService);

        assertEquals(List.of(neighbourService), service.getAllServices());
    }

    private DatabaseConfig initializedDatabase(String fileName) {
        DatabaseConfig config = new TestDatabaseConfig(tempDir.resolve(fileName));
        new SchemaInitializer(config).initialize();
        return config;
    }

    private static class TestDatabaseConfig extends DatabaseConfig {

        private final Path databasePath;

        private TestDatabaseConfig(Path databasePath) {
            this.databasePath = databasePath;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        }
    }
}
