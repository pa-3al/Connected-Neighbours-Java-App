package com.app.infrastructure.sync;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

public class IncidentSqliteGateway {

    private static final String REPORTS_TABLE = "reports";
    private static final String LEGACY_INCIDENTS_TABLE = "incidents";

    private final DatabaseConfig databaseConfig;

    public IncidentSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Connection openServerConnection(Path databasePath) throws SQLException {
        String pathStr = databasePath.toAbsolutePath().toString().replace('\\', '/');
        if (!pathStr.startsWith("/")) {
            pathStr = "/" + pathStr;
        }
        return DriverManager.getConnection("jdbc:sqlite:" + pathStr);
    }

    public void ensureSyncSchema(Connection localConnection, Connection serverConnection) throws SQLException {
        ensureUsersTable(localConnection);
        ensureIncidentsTable(serverConnection);
        ensureUsersTable(serverConnection);
        syncUsers(localConnection, serverConnection);
    }

    public Map<String, Incident> loadIncidentsById(Connection conn) throws SQLException {
        Map<String, Incident> incidents = new HashMap<>();
        String sql = "SELECT * FROM " + REPORTS_TABLE;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Incident incident = mapRow(rs);
                if (incident.id() != null && !incident.id().isBlank()) {
                    incidents.put(incident.id(), incident);
                }
            }
        }
        return incidents;
    }

    public void upsertIncident(Connection conn, Incident incident) throws SQLException {
        String sql = """
            INSERT INTO %s (id, title, description, category, status, reported_by_user_id, reported_by, admin_response_message, reported_at, resolved_at, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                category = excluded.category,
                status = excluded.status,
                reported_by_user_id = excluded.reported_by_user_id,
                reported_by = excluded.reported_by,
                admin_response_message = excluded.admin_response_message,
                reported_at = excluded.reported_at,
                resolved_at = excluded.resolved_at,
                last_modified = excluded.last_modified,
                sync_status = excluded.sync_status
            """.formatted(REPORTS_TABLE);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, incident.id());
            stmt.setString(2, incident.title());
            stmt.setString(3, incident.description());
            stmt.setString(4, incident.category() == null ? null : incident.category().name());
            stmt.setString(5, incident.status() == null ? null : incident.status().name());
            stmt.setString(6, incident.reportedByUserId());
            stmt.setString(7, incident.reportedBy());
            stmt.setString(8, incident.adminResponseMessage());
            stmt.setTimestamp(9, toTimestamp(incident.reportedAt()));
            stmt.setTimestamp(10, toTimestamp(incident.resolvedAt()));
            stmt.setTimestamp(11, toTimestamp(incident.lastModified()));
            stmt.setString(12, incident.syncStatus() == null ? null : incident.syncStatus().name());
            stmt.executeUpdate();
        }
    }

    private Incident mapRow(ResultSet rs) throws SQLException {
        return new Incident(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("description"),
                parseEnum(IncidentCategory.class, rs.getString("category")),
                parseEnum(IncidentStatus.class, rs.getString("status")),
                rs.getString("reported_by_user_id"),
                rs.getString("reported_by"),
                rs.getString("admin_response_message"),
                parseDbDate(rs.getString("reported_at")),
                parseDbDate(rs.getString("resolved_at")),
                parseDbDate(rs.getString("last_modified")),
                parseEnum(SyncStatus.class, rs.getString("sync_status"))
        );
    }

    private void ensureIncidentsTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            String createReportsSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description CLOB,
                    category VARCHAR(50),
                    status VARCHAR(50),
                    reported_by_user_id VARCHAR(36),
                    reported_by VARCHAR(255),
                    admin_response_message CLOB,
                    reported_at TIMESTAMP,
                    resolved_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """.formatted(REPORTS_TABLE);
            stmt.execute(createReportsSql);

            String createLegacyIncidentsSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description CLOB,
                    category VARCHAR(50),
                    status VARCHAR(50),
                    reported_by_user_id VARCHAR(36),
                    reported_by VARCHAR(255),
                    admin_response_message CLOB,
                    reported_at TIMESTAMP,
                    resolved_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """.formatted(LEGACY_INCIDENTS_TABLE);
            stmt.execute(createLegacyIncidentsSql);

            if (!hasColumn(conn, REPORTS_TABLE, "reported_by_user_id")) {
                stmt.execute("ALTER TABLE " + REPORTS_TABLE + " ADD COLUMN reported_by_user_id VARCHAR(36)");
            }

            String migrateLegacySql = """
                INSERT INTO %s (id, title, description, category, status, reported_by_user_id, reported_by, admin_response_message, reported_at, resolved_at, last_modified, sync_status)
                SELECT i.id, i.title, i.description, i.category, i.status, i.reported_by_user_id, i.reported_by, i.admin_response_message, i.reported_at, i.resolved_at, i.last_modified, i.sync_status
                FROM %s i
                WHERE NOT EXISTS (SELECT 1 FROM %s r WHERE r.id = i.id)
            """.formatted(REPORTS_TABLE, LEGACY_INCIDENTS_TABLE, REPORTS_TABLE);
            stmt.execute(migrateLegacySql);
        }
    }

    private void ensureUsersTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(36) PRIMARY KEY,
                    email VARCHAR(255),
                    firstname VARCHAR(255),
                    lastname VARCHAR(255),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            if (!hasColumn(conn, "users", "last_modified")) {
                stmt.execute("ALTER TABLE users ADD COLUMN last_modified TIMESTAMP");
            }
            if (!hasColumn(conn, "users", "sync_status")) {
                stmt.execute("ALTER TABLE users ADD COLUMN sync_status VARCHAR(50)");
            }
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void syncUsers(Connection localConnection, Connection serverConnection) throws SQLException {
        Map<String, UserRow> localUsers = loadUsers(localConnection);
        Map<String, UserRow> serverUsers = loadUsers(serverConnection);

        Set<String> allIds = new TreeSet<>();
        allIds.addAll(localUsers.keySet());
        allIds.addAll(serverUsers.keySet());

        for (String id : allIds) {
            UserRow local = localUsers.get(id);
            UserRow server = serverUsers.get(id);

            if (local == null && server != null) {
                upsertUser(localConnection, server.withSync(LocalDateTime.now()));
                continue;
            }

            if (local != null && server == null) {
                upsertUser(serverConnection, local.withSync(LocalDateTime.now()));
                continue;
            }

            if (local == null || server == null) {
                continue;
            }

            if (usersEquivalent(local, server)) {
                LocalDateTime merged = maxDate(local.lastModified(), server.lastModified());
                upsertUser(localConnection, local.withSync(merged));
                upsertUser(serverConnection, server.withSync(merged));
                continue;
            }

            UserRow winner = preferMostRecent(local, server);
            UserRow synced = winner.withSync(LocalDateTime.now());
            upsertUser(localConnection, synced);
            upsertUser(serverConnection, synced);
        }
    }

    private Map<String, UserRow> loadUsers(Connection conn) throws SQLException {
        Map<String, UserRow> users = new HashMap<>();
        String sql = "SELECT * FROM users";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UserRow user = new UserRow(
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("firstname"),
                        rs.getString("lastname"),
                        parseDbDate(rs.getString("created_at")),
                        parseDbDate(rs.getString("updated_at")),
                        parseDbDate(rs.getString("last_modified")),
                        parseEnum(SyncStatus.class, rs.getString("sync_status"))
                );
                if (user.id() != null && !user.id().isBlank()) {
                    users.put(user.id(), user);
                }
            }
        }

        return users;
    }

    private void upsertUser(Connection conn, UserRow user) throws SQLException {
        String sql = """
            INSERT INTO users (id, email, firstname, lastname, created_at, updated_at, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                email = excluded.email,
                firstname = excluded.firstname,
                lastname = excluded.lastname,
                created_at = excluded.created_at,
                updated_at = excluded.updated_at,
                last_modified = excluded.last_modified,
                sync_status = excluded.sync_status
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.id());
            stmt.setString(2, user.email());
            stmt.setString(3, user.firstname());
            stmt.setString(4, user.lastname());
            stmt.setTimestamp(5, toTimestamp(user.createdAt()));
            stmt.setTimestamp(6, toTimestamp(user.updatedAt()));
            stmt.setTimestamp(7, toTimestamp(user.lastModified()));
            stmt.setString(8, user.syncStatus() == null ? null : user.syncStatus().name());
            stmt.executeUpdate();
        }
    }

    private boolean usersEquivalent(UserRow first, UserRow second) {
        return Objects.equals(first.id(), second.id())
                && Objects.equals(first.email(), second.email())
                && Objects.equals(first.firstname(), second.firstname())
                && Objects.equals(first.lastname(), second.lastname());
    }

    private UserRow preferMostRecent(UserRow local, UserRow server) {
        LocalDateTime localTs = local.lastModified();
        LocalDateTime serverTs = server.lastModified();

        if (localTs == null && serverTs == null) {
            return local;
        }
        if (localTs == null) {
            return server;
        }
        if (serverTs == null) {
            return local;
        }
        return serverTs.isAfter(localTs) ? server : local;
    }

    private LocalDateTime maxDate(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b == null ? LocalDateTime.now() : b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private Timestamp toTimestamp(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }

    private LocalDateTime parseDbDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            if (dateStr.matches("^\\d+$")) {
                return normalizeDateTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(Long.parseLong(dateStr)),
                        ZoneId.systemDefault()
                ));
            }
            String normalized = dateStr.replace(' ', 'T');
            return normalizeDateTime(LocalDateTime.parse(normalized));
        } catch (NumberFormatException | DateTimeParseException e) {
            return null;
        }
    }

    private LocalDateTime normalizeDateTime(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record UserRow(
            String id,
            String email,
            String firstname,
            String lastname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastModified,
            SyncStatus syncStatus
    ) {
        UserRow withSync(LocalDateTime syncTime) {
            return new UserRow(
                    id,
                    email,
                    firstname,
                    lastname,
                    createdAt,
                    updatedAt,
                    syncTime,
                    SyncStatus.SYNCED
            );
        }
    }
}
