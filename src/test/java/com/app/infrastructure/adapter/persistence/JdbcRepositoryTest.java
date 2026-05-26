package com.app.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.app.domain.model.Alert;
import com.app.domain.model.Alert.AlertSeverity;
import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.model.Neighbourhood;
import com.app.domain.model.SyncStatus;
import com.app.domain.model.User;

class JdbcRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 26, 11, 0);

    @TempDir
    Path tempDir;

    @Test
    void jdbcUserRepositoryShouldSaveFindAndUpdateUsers() {
        DatabaseConfig config = initializedDatabase("users.sqlite");
        JdbcUserRepository repository = new JdbcUserRepository(config);

        repository.saveAll(List.of(new User("user-1", "Ada", "Lovelace", "ada@test.com")));

        assertEquals(new User("user-1", "Ada", "Lovelace", "ada@test.com"), repository.findById("user-1").orElseThrow());
        assertEquals(List.of(new User("user-1", "Ada", "Lovelace", "ada@test.com")), repository.findAll());

        repository.saveAll(List.of(new User("user-1", "Grace", "Hopper", "grace@test.com")));

        assertEquals(new User("user-1", "Grace", "Hopper", "grace@test.com"), repository.findById("user-1").orElseThrow());
    }

    @Test
    void jdbcAlertRepositoryShouldSaveFindActiveAndDeleteAlerts() {
        DatabaseConfig config = initializedDatabase("alerts.sqlite");
        JdbcAlertRepository repository = new JdbcAlertRepository(config);
        Alert active = new Alert("alert-1", "Road work", "Street closed", AlertSeverity.WARNING, NOW, NOW.plusDays(1), true, "admin", NOW, SyncStatus.LOCAL_ONLY);
        Alert inactive = new Alert("alert-2", "Old alert", "Expired", AlertSeverity.INFO, NOW.minusDays(2), NOW.minusDays(1), false, "admin", NOW.minusDays(1), SyncStatus.SYNCED);

        repository.save(active);
        repository.save(inactive);

        assertEquals(2, repository.findAll().size());
        assertEquals(List.of(active), repository.findActive());
        assertEquals(active, repository.findById("alert-1").orElseThrow());

        repository.deleteById("alert-1");

        assertTrue(repository.findById("alert-1").isEmpty());
        assertEquals(List.of(), repository.findActive());
    }

    @Test
    void jdbcAlertRepositoryShouldMapInvalidEnumsToNull() throws Exception {
        DatabaseConfig config = initializedDatabase("invalid-alert.sqlite");
        execute(config, "INSERT INTO alerts (id, title, message, severity, is_active, sync_status) VALUES ('alert-1', 'Title', 'Message', 'UNKNOWN', 1, 'BAD_STATUS')");
        JdbcAlertRepository repository = new JdbcAlertRepository(config);

        Alert alert = repository.findById("alert-1").orElseThrow();

        assertNull(alert.severity());
        assertNull(alert.syncStatus());
    }

    @Test
    void jdbcIncidentRepositoryShouldSaveFindByStatusAndDeleteIncidents() {
        DatabaseConfig config = initializedDatabase("incidents.sqlite");
        new JdbcUserRepository(config).saveAll(List.of(new User("user-1", "Ada", "Lovelace", "ada@test.com")));
        JdbcIncidentRepository repository = new JdbcIncidentRepository(config);
        Incident incident = new Incident("incident-1", "Broken gate", "Gate cannot close", IncidentCategory.OTHER, IncidentStatus.PENDING, "user-1", "fallback", "pending review", NOW.minusHours(2), null, NOW, SyncStatus.LOCAL_ONLY);

        repository.save(incident);

        Incident found = repository.findById("incident-1").orElseThrow();
        assertEquals("Ada Lovelace", found.reportedBy());
        assertEquals(List.of(found), repository.findByStatus(IncidentStatus.PENDING));

        repository.deleteById("incident-1");

        assertTrue(repository.findById("incident-1").isEmpty());
    }

    @Test
    void jdbcIncidentRepositoryShouldFallbackToReportedByWhenUserNameIsMissing() {
        DatabaseConfig config = initializedDatabase("incident-fallback.sqlite");
        JdbcIncidentRepository repository = new JdbcIncidentRepository(config);
        Incident incident = new Incident("incident-1", "Broken gate", "Gate cannot close", IncidentCategory.OTHER, IncidentStatus.PENDING, "missing-user", "Neighbour", null, NOW.minusHours(2), null, NOW, SyncStatus.LOCAL_ONLY);

        repository.save(incident);

        assertEquals("Neighbour", repository.findById("incident-1").orElseThrow().reportedBy());
    }

    @Test
    void jdbcEventStatsRepositoryShouldAggregateEvents() throws Exception {
        DatabaseConfig config = initializedDatabase("event-stats.sqlite");
        execute(config, "INSERT INTO events (id, name, description, real_money_price, last_modified) VALUES ('event-1', 'Garden day', 'Planting', 0, '2026-05-01T10:00:00')");
        execute(config, "INSERT INTO events (id, name, description, real_money_price, last_modified) VALUES ('event-2', 'Picnic', 'Lunch', 3, '2026-05-15T10:00:00')");
        execute(config, "INSERT INTO events (id, name, description, real_money_price, last_modified) VALUES ('event-3', 'Workshop', 'Repair', 20, '2026-06-01T10:00:00')");
        execute(config, "INSERT INTO event_participations (id, user_id, event_id) VALUES ('participation-1', 'user-1', 'event-1')");
        execute(config, "INSERT INTO event_participations (id, user_id, event_id) VALUES ('participation-2', 'user-2', 'event-1')");
        execute(config, "INSERT INTO event_participations (id, user_id, event_id) VALUES ('participation-3', 'user-3', 'event-2')");
        JdbcEventStatsRepository repository = new JdbcEventStatsRepository(config);

        assertEquals(Map.of("2026-05", 2, "2026-06", 1), repository.getEventsByMonth());
        assertEquals(2, repository.getTopEventsByParticipation().get("Garden day"));
        assertEquals(1, repository.getTopEventsByParticipation().get("Picnic"));
        assertEquals(1, repository.getEventsByPriceType().get("Gratuit"));
        assertEquals(2, repository.getEventsByPriceType().get("Payant"));
        assertEquals(3, repository.getEventPriceDistribution().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(1.5, repository.getAveragePriceByMonth().get("2026-05"), 0.001);
        assertEquals(20.0, repository.getAveragePriceByMonth().get("2026-06"), 0.001);
    }

    @Test
    void jdbcServiceRepositoryShouldCountStatusesAndExpectedDatesByMonth() throws Exception {
        DatabaseConfig config = initializedDatabase("service-stats.sqlite");
        execute(config, "INSERT INTO services (id, title, description, status, created_by_user_id) VALUES ('service-1', 'Groceries', 'Carry groceries', 'PENDING', 'user-1')");
        execute(config, "INSERT INTO services (id, title, description, status, created_by_user_id) VALUES ('service-2', 'Drive', 'Drive to station', 'PENDING', 'user-2')");
        execute(config, "INSERT INTO services (id, title, description, status, created_by_user_id) VALUES ('service-3', 'Repair', 'Repair shelf', 'DONE', 'user-3')");
        execute(config, "INSERT INTO service_expected_dates (id, start_date, service_id) VALUES ('date-1', '2026-05-01 08:00:00', 'service-1')");
        execute(config, "INSERT INTO service_expected_dates (id, start_date, service_id) VALUES ('date-2', '2026-05-15T08:00:00', 'service-2')");
        execute(config, "INSERT INTO service_expected_dates (id, start_date, service_id) VALUES ('date-3', '2026-06-01T08:00:00', 'service-3')");
        JdbcServiceRepository repository = new JdbcServiceRepository(config);

        assertEquals(2, repository.countServicesByStatus().get("PENDING"));
        assertEquals(1, repository.countServicesByStatus().get("DONE"));
        assertEquals(2, repository.countExpectedDatesByMonth().get("2026-05"));
        assertEquals(1, repository.countExpectedDatesByMonth().get("2026-06"));
    }

    @Test
    void jdbcAddressRepositoryShouldCountAddressesAndUsersPerNeighbourhood() throws Exception {
        DatabaseConfig config = initializedDatabase("address-stats.sqlite");
        execute(config, "INSERT INTO neighbourhoods (id, name, city, country_code) VALUES ('neighbourhood-1', 'Centre', 'Paris', 'FR')");
        execute(config, "INSERT INTO neighbourhoods (id, name, city, country_code) VALUES ('neighbourhood-2', 'North', 'Paris', 'FR')");
        execute(config, "INSERT INTO addresses (id, street_number, street_name, city, postal_code, country_code, neighbourhood_id, user_id) VALUES ('address-1', '1', 'Main', 'Paris', '75001', 'FR', 'neighbourhood-1', 'user-1')");
        execute(config, "INSERT INTO addresses (id, street_number, street_name, city, postal_code, country_code, neighbourhood_id, user_id) VALUES ('address-2', '2', 'Main', 'Paris', '75001', 'FR', 'neighbourhood-1', 'user-1')");
        execute(config, "INSERT INTO addresses (id, street_number, street_name, city, postal_code, country_code, neighbourhood_id, user_id) VALUES ('address-3', '3', 'High', 'Brussels', '1000', 'BE', 'neighbourhood-2', 'user-2')");
        JdbcAddressRepository repository = new JdbcAddressRepository(config);

        assertEquals(2, repository.countAddressesPerCountry().get("FR"));
        assertEquals(1, repository.countAddressesPerCountry().get("BE"));
        assertEquals(1, repository.countUsersPerNeighbourhood().get("Centre"));
        assertEquals(1, repository.countUsersPerNeighbourhood().get("North"));
    }

    @Test
    void jdbcNeighbourhoodRepositoryShouldMapOptionalFieldsAndEpochLastModified() throws Exception {
        DatabaseConfig config = initializedDatabase("neighbourhood-repository.sqlite");
        long timestamp = 1_779_790_800_000L;
        try (Connection connection = config.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO neighbourhoods (id, name, description, city, postal_code, country_code, estimated_population, polygon, area, last_modified, sync_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, "neighbourhood-1");
            statement.setString(2, "Centre");
            statement.setString(3, "Downtown");
            statement.setString(4, "Paris");
            statement.setString(5, "75001");
            statement.setString(6, "FR");
            statement.setInt(7, 1200);
            statement.setString(8, "POLYGON((0 0,1 1))");
            statement.setInt(9, 42);
            statement.setLong(10, timestamp);
            statement.setString(11, SyncStatus.SYNCED.name());
            statement.executeUpdate();
        }
        JdbcNeighbourhoodRepository repository = new JdbcNeighbourhoodRepository(config);

        Neighbourhood neighbourhood = repository.findAll().getFirst();

        assertEquals("neighbourhood-1", neighbourhood.id());
        assertEquals(1200, neighbourhood.estimatedPopulation());
        assertEquals(42, neighbourhood.area());
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()), neighbourhood.lastModified());
        assertEquals(SyncStatus.SYNCED, neighbourhood.syncStatus());
    }

    @Test
    void schemaInitializerShouldCreateCoreTables() throws Exception {
        DatabaseConfig config = initializedDatabase("schema.sqlite");

        assertTrue(tableExists(config, "users"));
        assertTrue(tableExists(config, "reports"));
        assertTrue(tableExists(config, "alerts"));
        assertTrue(tableExists(config, "events"));
        assertTrue(tableExists(config, "services"));
        assertFalse(tableExists(config, "missing_table"));
    }

    private DatabaseConfig initializedDatabase(String fileName) {
        DatabaseConfig config = new TestDatabaseConfig(tempDir.resolve(fileName));
        new SchemaInitializer(config).initialize();
        return config;
    }

    private void execute(DatabaseConfig config, String sql) throws SQLException {
        try (Connection connection = config.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean tableExists(DatabaseConfig config, String tableName) throws SQLException {
        try (Connection connection = config.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, tableName);
            return statement.executeQuery().next();
        }
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
