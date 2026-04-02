package com.app.infrastructure.sync;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentPriority;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.IncidentService;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.util.DailyLogger;

public class IncidentSyncManager {

    private final IncidentService incidentService;
    private final DatabaseConfig databaseConfig;
    private final ConfigProvider configProvider;

    public IncidentSyncManager(IncidentService incidentService) {
        this(incidentService, new DatabaseConfig());
    }

    public IncidentSyncManager(IncidentService incidentService, DatabaseConfig databaseConfig) {
        this.incidentService = incidentService;
        this.databaseConfig = databaseConfig;
        this.configProvider = new ConfigProvider();
    }

    public IncidentSyncReport syncWithBackend(Function<IncidentConflict, Incident> conflictResolver) {
        Objects.requireNonNull(conflictResolver, "conflictResolver must not be null");

        Map<String, Incident> localById = toMapById(incidentService.getAllIncidents());
        Map<String, Incident> serverById = toMapById(fetchServerIncidentsFromBackend());
        int pushedToServer = 0;
        int pulledFromServer = 0;
        int conflictsResolved = 0;
        int conflictsUnresolved = 0;
        int unchanged = 0;

        Set<String> allIds = new TreeSet<>();
        allIds.addAll(localById.keySet());
        allIds.addAll(serverById.keySet());

        for (String id : allIds) {
            Incident local = localById.get(id);
            Incident server = serverById.get(id);

            if (local == null && server != null) {
                Incident synced = withSyncMetadata(server, LocalDateTime.now());
                incidentService.updateIncident(synced);
                pulledFromServer++;
                continue;
            }

            if (local != null && server == null) {
                unchanged++;
                continue;
            }

            if (local == null || server == null) {
                continue;
            }

            if (areEquivalent(local, server)) {
                Incident synced = withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified()));
                incidentService.updateIncident(synced);
                unchanged++;
                continue;
            }

            Incident resolved = conflictResolver.apply(new IncidentConflict(local, server));
            if (resolved == null) {
                incidentService.updateIncident(local.withSyncStatus(SyncStatus.CONFLICT));
                conflictsUnresolved++;
                continue;
            }

            Incident synced = withSyncMetadata(resolved, LocalDateTime.now());
            incidentService.updateIncident(synced);
            conflictsResolved++;
        }

        return new IncidentSyncReport(
            pushedToServer,
            pulledFromServer,
            conflictsResolved,
            conflictsUnresolved,
            unchanged
        );
    }

    public IncidentSyncReport sync(Path serverDatabasePath, Function<IncidentConflict, Incident> conflictResolver) {
        Objects.requireNonNull(serverDatabasePath, "serverDatabasePath must not be null");
        Objects.requireNonNull(conflictResolver, "conflictResolver must not be null");

        Map<String, Incident> localById = toMapById(incidentService.getAllIncidents());

        try (Connection localConnection = databaseConfig.getConnection();
             Connection serverConnection = openConnection(serverDatabasePath)) {
            ensureUsersTable(localConnection);
            ensureIncidentsTable(serverConnection);
            ensureUsersTable(serverConnection);

            syncUsers(localConnection, serverConnection);

            Map<String, Incident> serverById = loadServerIncidents(serverConnection);

            int pushedToServer = 0;
            int pulledFromServer = 0;
            int conflictsResolved = 0;
            int conflictsUnresolved = 0;
            int unchanged = 0;

            Set<String> allIds = new TreeSet<>();
            allIds.addAll(localById.keySet());
            allIds.addAll(serverById.keySet());

            for (String id : allIds) {
                Incident local = localById.get(id);
                Incident server = serverById.get(id);

                if (local == null && server != null) {
                    Incident synced = withSyncMetadata(server, LocalDateTime.now());
                    incidentService.updateIncident(synced);
                    pulledFromServer++;
                    continue;
                }

                if (local != null && server == null) {
                    Incident synced = withSyncMetadata(local, LocalDateTime.now());
                    incidentService.updateIncident(synced);
                    upsertIncident(serverConnection, synced);
                    pushedToServer++;
                    continue;
                }

                if (local == null) {
                    continue;
                }

                if (server == null) {
                    continue;
                }

                if (areEquivalent(local, server)) {
                    Incident synced = withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified()));
                    incidentService.updateIncident(synced);
                    unchanged++;
                    continue;
                }

                Incident resolved = conflictResolver.apply(new IncidentConflict(local, server));
                if (resolved == null) {
                    incidentService.updateIncident(local.withSyncStatus(SyncStatus.CONFLICT));
                    conflictsUnresolved++;
                    continue;
                }

                Incident synced = withSyncMetadata(resolved, LocalDateTime.now());
                incidentService.updateIncident(synced);
                upsertIncident(serverConnection, synced);
                conflictsResolved++;
            }

            return new IncidentSyncReport(
                pushedToServer,
                pulledFromServer,
                conflictsResolved,
                conflictsUnresolved,
                unchanged
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to synchronize incidents", e);
        }
    }

    private Map<String, Incident> toMapById(java.util.List<Incident> incidents) {
        Map<String, Incident> byId = new HashMap<>();
        for (Incident incident : incidents) {
            if (incident != null && incident.id() != null && !incident.id().isBlank()) {
                byId.put(incident.id(), incident);
            }
        }
        return byId;
    }

    private List<Incident> fetchServerIncidentsFromBackend() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new RuntimeException("Missing app.sync.db.url in application.properties");
        }

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        DailyLogger.logInfo("Sync", "Fetching incidents from direct DB: " + jdbcUrl.replaceAll("://([^:]+):([^@]+)@", "://$1:***@"));

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            logDatabaseIdentity(conn);
            logKeyTableCounts(conn);
            String table = resolveIncidentTable(conn);
            logRlsStatus(conn, table);
            String sql = "SELECT * FROM " + table;

            java.util.ArrayList<Incident> result = new java.util.ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    result.add(mapIncidentRow(rs, meta));
                }
            }

            DailyLogger.logInfo("Sync", "Fetched " + result.size() + " incidents from table " + table);
            return result;
        } catch (SQLException e) {
            Throwable cause = e.getCause();
            String root = cause == null ? "" : " cause=" + cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage());
            DailyLogger.logError("Sync", "Failed to fetch incidents from direct DB: sqlState=" + e.getSQLState() + " errorCode=" + e.getErrorCode() + " message=" + e.getMessage() + root, e);
            throw new RuntimeException("Failed to fetch incidents from direct DB", e);
        }
    }

    private String toJdbcPostgresUrl(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return trimmed;
        }
        if (trimmed.startsWith("postgresql://") || trimmed.startsWith("postgres://")) {
            String normalized = trimmed.startsWith("postgres://")
                ? "postgresql://" + trimmed.substring("postgres://".length())
                : trimmed;

            URI uri = URI.create(normalized);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String db = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(db);

            StringBuilder params = new StringBuilder();
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                params.append(uri.getRawQuery());
            }

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                String user = parts.length > 0 ? parts[0] : "";
                String pass = parts.length > 1 ? parts[1] : "";
                if (!user.isBlank()) {
                    if (params.length() > 0) params.append("&");
                    params.append("user=").append(URLEncoder.encode(user, StandardCharsets.UTF_8));
                }
                if (!pass.isBlank()) {
                    if (params.length() > 0) params.append("&");
                    params.append("password=").append(URLEncoder.encode(pass, StandardCharsets.UTF_8));
                }
            }

            if (params.length() > 0) {
                jdbc.append("?").append(params);
            }
            return jdbc.toString();
        }
        return trimmed;
    }

    private String resolveIncidentTable(Connection conn) throws SQLException {
        String configuredTable = configProvider.getSyncDatabaseTable();
        String[] targetNames = (configuredTable != null && !configuredTable.isBlank())
            ? new String[] {configuredTable}
            : new String[] {"incidents", "incident_reports"};
        TableCandidate best = null;

        String sql = """
            SELECT table_schema, table_name
            FROM information_schema.tables
            WHERE table_name = ?
              AND table_schema NOT IN ('pg_catalog', 'information_schema')
            ORDER BY CASE WHEN table_schema = 'public' THEN 0 ELSE 1 END, table_schema
            """;

        for (String targetName : targetNames) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String schema = rs.getString("table_schema");
                        String table = rs.getString("table_name");
                        long count = countRows(conn, schema, table);
                        DailyLogger.logInfo("Sync", "Table candidate " + schema + "." + table + " has " + count + " row(s)");

                        if (best == null || count > best.rowCount) {
                            best = new TableCandidate(schema, table, count);
                        }
                    }
                }
            }
        }

        if (best != null) {
            return quoteIdent(best.schema) + "." + quoteIdent(best.table);
        }

        if (configuredTable == null || configuredTable.isBlank()) {
            DailyLogger.logWarn(
                "Sync",
                "No reports table found (incidents/incident_reports). Set app.sync.db.table in application.properties if you want another source table."
            );
        }

        String available = String.join(", ", listBusinessTables(conn));
        throw new SQLException(
            "No compatible table found (expected incidents or incident_reports"
                + ((configuredTable != null && !configuredTable.isBlank()) ? ", or configured table '" + configuredTable + "'" : "")
                + "). Available tables: "
                + available
        );
    }

    private void logDatabaseIdentity(Connection conn) {
        String sql = "SELECT current_database() AS db, current_user AS usr, current_schema() AS schema";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                DailyLogger.logInfo(
                    "Sync",
                    "Connected as user='" + rs.getString("usr")
                        + "' db='" + rs.getString("db")
                        + "' schema='" + rs.getString("schema") + "'"
                );
            }
        } catch (SQLException e) {
            DailyLogger.logWarn("Sync", "Unable to read DB identity: " + e.getMessage());
        }
    }

    private void logKeyTableCounts(Connection conn) {
        String sql = """
            SELECT table_schema, table_name
            FROM information_schema.tables
            WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
              AND table_name IN ('users', 'moderators', 'admins', 'incidents', 'incident_reports')
            ORDER BY table_schema, table_name
            """;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String schema = rs.getString("table_schema");
                String table = rs.getString("table_name");
                long count = countRows(conn, schema, table);
                DailyLogger.logInfo("Sync", "Key table " + schema + "." + table + " count=" + count);
            }
        } catch (SQLException e) {
            DailyLogger.logWarn("Sync", "Unable to log key table counts: " + e.getMessage());
        }
    }

    private void logRlsStatus(Connection conn, String qualifiedTable) {
        String cleaned = qualifiedTable.replace("\"", "");
        String[] parts = cleaned.split("\\.", 2);
        if (parts.length != 2) {
            return;
        }

        String sql = """
            SELECT c.relrowsecurity
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = ? AND c.relname = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parts[0]);
            ps.setString(2, parts[1]);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DailyLogger.logInfo("Sync", "RLS enabled on " + cleaned + " = " + rs.getBoolean("relrowsecurity"));
                }
            }
        } catch (SQLException e) {
            DailyLogger.logWarn("Sync", "Unable to check RLS for " + cleaned + ": " + e.getMessage());
        }
    }

    private long countRows(Connection conn, String schema, String table) {
        String sql = "SELECT COUNT(*) FROM " + quoteIdent(schema) + "." + quoteIdent(table);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            DailyLogger.logWarn("Sync", "Unable to count rows for " + schema + "." + table + ": " + e.getMessage());
            return 0;
        }
    }

    private String quoteIdent(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private static final class TableCandidate {
        private final String schema;
        private final String table;
        private final long rowCount;

        private TableCandidate(String schema, String table, long rowCount) {
            this.schema = schema;
            this.table = table;
            this.rowCount = rowCount;
        }
    }

    private java.util.List<String> listBusinessTables(Connection conn) throws SQLException {
        String sql = """
            SELECT table_schema || '.' || table_name AS full_table
            FROM information_schema.tables
            WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
            ORDER BY table_schema, table_name
            """;

        java.util.ArrayList<String> tables = new java.util.ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString("full_table"));
            }
        }
        return tables;
    }

    private Incident mapIncidentRow(ResultSet rs, ResultSetMetaData meta) throws SQLException {
        String descriptionCol = hasColumn(meta, "description")
            ? "description"
            : (hasColumn(meta, "content") ? "content" : (hasColumn(meta, "motivation") ? "motivation" : null));
        String reportedByUserIdCol = hasColumn(meta, "reported_by_user_id") ? "reported_by_user_id" : (hasColumn(meta, "user_id") ? "user_id" : null);
        String reportedByCol = hasColumn(meta, "reported_by") ? "reported_by" : null;

        String title = getStringIfPresent(rs, meta, "title");
        if ((title == null || title.isBlank()) && hasColumn(meta, "motivation")) {
            title = "Moderator request";
        }

        return new Incident(
            getStringIfPresent(rs, meta, "id"),
            title,
            descriptionCol == null ? null : rs.getString(descriptionCol),
            parseEnum(IncidentCategory.class, getStringIfPresent(rs, meta, "category")),
            parseEnum(IncidentStatus.class, getStringIfPresent(rs, meta, "status")),
            parseEnum(IncidentPriority.class, getStringIfPresent(rs, meta, "priority")),
            reportedByUserIdCol == null ? null : rs.getString(reportedByUserIdCol),
            reportedByCol == null ? null : rs.getString(reportedByCol),
            getStringIfPresent(rs, meta, "location"),
            getDateTimeWithFallback(rs, meta, "reported_at", "created_at"),
            getDateTimeIfPresent(rs, meta, "resolved_at"),
            getDateTimeWithFallback(rs, meta, "last_modified", "updated_at"),
            parseEnum(SyncStatus.class, getStringIfPresent(rs, meta, "sync_status"))
        );
    }

    private boolean hasColumn(ResultSetMetaData meta, String columnName) throws SQLException {
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(meta.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    private String getStringIfPresent(ResultSet rs, ResultSetMetaData meta, String column) throws SQLException {
        return hasColumn(meta, column) ? rs.getString(column) : null;
    }

    private LocalDateTime getDateTimeIfPresent(ResultSet rs, ResultSetMetaData meta, String column) throws SQLException {
        if (!hasColumn(meta, column)) {
            return null;
        }
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    private LocalDateTime getDateTimeWithFallback(
        ResultSet rs,
        ResultSetMetaData meta,
        String preferred,
        String fallback
    ) throws SQLException {
        LocalDateTime value = getDateTimeIfPresent(rs, meta, preferred);
        if (value != null) {
            return value;
        }
        return getDateTimeIfPresent(rs, meta, fallback);
    }

    private Connection openConnection(Path databasePath) throws SQLException {
        String pathStr = databasePath.toAbsolutePath().toString().replace('\\', '/');
        if (!pathStr.startsWith("/")) {
            pathStr = "/" + pathStr;
        }
        return DriverManager.getConnection("jdbc:sqlite:" + pathStr);
    }

    private void ensureIncidentsTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS incidents (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description CLOB,
                    category VARCHAR(50),
                    status VARCHAR(50),
                    priority VARCHAR(50),
                    reported_by_user_id VARCHAR(36),
                    reported_by VARCHAR(255),
                    location VARCHAR(255),
                    reported_at TIMESTAMP,
                    resolved_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            if (!hasColumn(conn, "incidents", "reported_by_user_id")) {
                stmt.execute("ALTER TABLE incidents ADD COLUMN reported_by_user_id VARCHAR(36)");
            }
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
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, Incident> loadServerIncidents(Connection conn) throws SQLException {
        Map<String, Incident> incidents = new HashMap<>();
        String sql = "SELECT * FROM incidents";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Incident incident = mapRow(rs);
                incidents.put(incident.id(), incident);
            }
        }
        return incidents;
    }

    private void upsertIncident(Connection conn, Incident incident) throws SQLException {
        String sql = """
            INSERT INTO incidents (id, title, description, category, status, priority, reported_by_user_id, reported_by, location, reported_at, resolved_at, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                category = excluded.category,
                status = excluded.status,
                priority = excluded.priority,
                reported_by_user_id = excluded.reported_by_user_id,
                reported_by = excluded.reported_by,
                location = excluded.location,
                reported_at = excluded.reported_at,
                resolved_at = excluded.resolved_at,
                last_modified = excluded.last_modified,
                sync_status = excluded.sync_status
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, incident.id());
            stmt.setString(2, incident.title());
            stmt.setString(3, incident.description());
            stmt.setString(4, incident.category() == null ? null : incident.category().name());
            stmt.setString(5, incident.status() == null ? null : incident.status().name());
            stmt.setString(6, incident.priority() == null ? null : incident.priority().name());
            stmt.setString(7, incident.reportedByUserId());
            stmt.setString(8, incident.reportedBy());
            stmt.setString(9, incident.location());
            stmt.setTimestamp(10, toTimestamp(incident.reportedAt()));
            stmt.setTimestamp(11, toTimestamp(incident.resolvedAt()));
            stmt.setTimestamp(12, toTimestamp(incident.lastModified()));
            stmt.setString(13, incident.syncStatus() == null ? null : incident.syncStatus().name());
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
            parseEnum(IncidentPriority.class, rs.getString("priority")),
            rs.getString("reported_by_user_id"),
            rs.getString("reported_by"),
            rs.getString("location"),
            parseDbDate(rs.getString("reported_at")),
            parseDbDate(rs.getString("resolved_at")),
            parseDbDate(rs.getString("last_modified")),
            parseEnum(SyncStatus.class, rs.getString("sync_status"))
        );
    }

    private Incident withSyncMetadata(Incident incident, LocalDateTime syncTime) {
        return new Incident(
            incident.id(),
            incident.title(),
            incident.description(),
            incident.category(),
            incident.status(),
            incident.priority(),
            incident.reportedByUserId(),
            incident.reportedBy(),
            incident.location(),
            incident.reportedAt(),
            incident.resolvedAt(),
            syncTime,
            SyncStatus.SYNCED
        );
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

    private boolean areEquivalent(Incident first, Incident second) {
        if (first == null || second == null) {
            return false;
        }
        return Objects.equals(first.id(), second.id())
            && Objects.equals(first.title(), second.title())
            && Objects.equals(first.description(), second.description())
            && Objects.equals(first.category(), second.category())
            && Objects.equals(first.status(), second.status())
            && Objects.equals(first.priority(), second.priority())
            && Objects.equals(first.reportedByUserId(), second.reportedByUserId())
            && Objects.equals(first.reportedBy(), second.reportedBy())
            && Objects.equals(first.location(), second.location())
            && Objects.equals(first.reportedAt(), second.reportedAt())
            && Objects.equals(first.resolvedAt(), second.resolvedAt());
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
                UserRow synced = server.withSync(LocalDateTime.now());
                upsertUser(localConnection, synced);
                continue;
            }

            if (local != null && server == null) {
                UserRow synced = local.withSync(LocalDateTime.now());
                upsertUser(serverConnection, synced);
                continue;
            }

            if (local == null) {
                continue;
            }

            if (server == null) {
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

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
        if (first == null || second == null) {
            return false;
        }

        return Objects.equals(first.id(), second.id())
            && Objects.equals(first.email(), second.email())
            && Objects.equals(first.firstname(), second.firstname())
            && Objects.equals(first.lastname(), second.lastname())
            && Objects.equals(first.createdAt(), second.createdAt())
            && Objects.equals(first.updatedAt(), second.updatedAt());
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

    private Timestamp toTimestamp(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }

    private LocalDateTime parseDbDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            if (dateStr.matches("^\\d+$")) {
                return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(Long.parseLong(dateStr)),
                    java.time.ZoneId.systemDefault()
                );
            }
            String normalized = dateStr.replace(' ', 'T');
            return LocalDateTime.parse(normalized);
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            System.err.println("Error parsing date in sync: " + dateStr + " - " + e.getMessage());
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
