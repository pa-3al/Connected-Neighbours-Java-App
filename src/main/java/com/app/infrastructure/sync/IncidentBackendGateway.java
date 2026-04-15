package com.app.infrastructure.sync;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.util.DailyLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class IncidentBackendGateway {

    private final ConfigProvider configProvider;
    private final AuthenticatedHttpClient authenticatedHttpClient;

    public IncidentBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = configProvider;
        this.authenticatedHttpClient = authenticatedHttpClient;
    }

    public Map<String, Incident> fetchIncidentsById() {
        List<Incident> incidents = fetchServerIncidentsFromBackend();
        Map<String, Incident> byId = new HashMap<>();
        for (Incident incident : incidents) {
            if (incident != null && incident.id() != null && !incident.id().isBlank()) {
                byId.put(incident.id(), incident);
            }
        }
        return byId;
    }

    public void pushAdminResponse(String id, String message) {
        try {
            String url = configProvider.getAuthBaseUrl() + "/admin/reports/" + id + "/response";

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            body.put("message", message);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = authenticatedHttpClient.send(request);
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                DailyLogger.logError("Sync", "Failed to push response for report " + id + ": " + response.body());
                return;
            }
            DailyLogger.logInfo("Sync", "Successfully pushed admin response for report " + id);
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Error pushing admin response", e);
        }
    }

    private List<Incident> fetchServerIncidentsFromBackend() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new RuntimeException("Missing app.sync.db.url in application.properties");
        }

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        DailyLogger.logInfo("Sync", "Fetching reports from direct DB: " + jdbcUrl.replaceAll("://([^:]+):([^@]+)@", "://$1:***@"));

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            logDatabaseIdentity(conn);
            logKeyTableCounts(conn);
            String table = resolveIncidentTable(conn);
            logRlsStatus(conn, table);
            String sql = "SELECT * FROM " + table;

            ArrayList<Incident> result = new ArrayList<>();
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    result.add(mapIncidentRow(rs, meta));
                }
            }

            DailyLogger.logInfo("Sync", "Fetched " + result.size() + " reports from table " + table);
            return result;
        } catch (SQLException e) {
            Throwable cause = e.getCause();
            String root = cause == null ? "" : " cause=" + cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage());
            DailyLogger.logError("Sync", "Failed to fetch reports from direct DB: sqlState=" + e.getSQLState() + " errorCode=" + e.getErrorCode() + " message=" + e.getMessage() + root, e);
            throw new RuntimeException("Failed to fetch reports from direct DB", e);
        }
    }

    private String toJdbcPostgresUrl(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return trimmed;
        }
        if (!trimmed.startsWith("postgresql://") && !trimmed.startsWith("postgres://")) {
            return trimmed;
        }

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
                if (params.length() > 0) {
                    params.append("&");
                }
                params.append("user=").append(URLEncoder.encode(user, StandardCharsets.UTF_8));
            }
            if (!pass.isBlank()) {
                if (params.length() > 0) {
                    params.append("&");
                }
                params.append("password=").append(URLEncoder.encode(pass, StandardCharsets.UTF_8));
            }
        }

        if (params.length() > 0) {
            jdbc.append("?").append(params);
        }
        return jdbc.toString();
    }

    private String resolveIncidentTable(Connection conn) throws SQLException {
        String configuredTable = configProvider.getSyncDatabaseTable();
        String[] targetNames = (configuredTable != null && !configuredTable.isBlank())
                ? new String[]{configuredTable}
                : new String[]{"reports", "incidents", "incident_reports"};

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
            DailyLogger.logWarn("Sync", "No reports table found (reports/incidents/incident_reports). Set app.sync.db.table in application.properties if you want another source table.");
        }

        String available = String.join(", ", listBusinessTables(conn));
        throw new SQLException(
                "No compatible table found (expected reports, incidents or incident_reports"
                        + ((configuredTable != null && !configuredTable.isBlank()) ? ", or configured table '" + configuredTable + "'" : "")
                        + "). Available tables: "
                        + available
        );
    }

    private void logDatabaseIdentity(Connection conn) {
        String sql = "SELECT current_database() AS db, current_user AS usr, current_schema() AS schema";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                DailyLogger.logInfo("Sync", "Connected as user='" + rs.getString("usr") + "' db='" + rs.getString("db") + "' schema='" + rs.getString("schema") + "'");
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
              AND table_name IN ('users', 'moderators', 'admins', 'reports', 'incidents', 'incident_reports')
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

    private List<String> listBusinessTables(Connection conn) throws SQLException {
        String sql = """
            SELECT table_schema || '.' || table_name AS full_table
            FROM information_schema.tables
            WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
            ORDER BY table_schema, table_name
            """;

        ArrayList<String> tables = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString("full_table"));
            }
        }
        return tables;
    }

    private Incident mapIncidentRow(ResultSet rs, ResultSetMetaData meta) throws SQLException {
        String descriptionCol = hasColumn(meta, "content") ? "content" : (hasColumn(meta, "description") ? "description" : null);
        String reportedByUserIdCol = hasColumn(meta, "user_id") ? "user_id" : (hasColumn(meta, "reported_by_user_id") ? "reported_by_user_id" : null);
        String categoryCol = hasColumn(meta, "theme") ? "theme" : (hasColumn(meta, "category") ? "category" : null);
        String reportedByCol = hasColumn(meta, "reported_by") ? "reported_by" : null;
        String adminResponseCol = hasColumn(meta, "admin_response_message") ? "admin_response_message" : null;

        return new Incident(
                getStringIfPresent(rs, meta, "id"),
                getStringIfPresent(rs, meta, "title"),
                descriptionCol == null ? null : rs.getString(descriptionCol),
                parseEnum(IncidentCategory.class, categoryCol == null ? null : rs.getString(categoryCol)),
                parseEnum(IncidentStatus.class, getStringIfPresent(rs, meta, "status")),
                reportedByUserIdCol == null ? null : rs.getString(reportedByUserIdCol),
                reportedByCol == null ? null : rs.getString(reportedByCol),
                adminResponseCol == null ? null : rs.getString(adminResponseCol),
                getDateTimeWithFallback(rs, meta, "created_at", "reported_at"),
                getDateTimeWithFallback(rs, meta, "responded_at", "resolved_at"),
                getDateTimeWithFallback(rs, meta, "updated_at", "last_modified"),
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

    private LocalDateTime getDateTimeWithFallback(ResultSet rs, ResultSetMetaData meta, String preferred, String fallback) throws SQLException {
        LocalDateTime value = getDateTimeIfPresent(rs, meta, preferred);
        if (value != null) {
            return value;
        }
        return getDateTimeIfPresent(rs, meta, fallback);
    }

    private LocalDateTime getDateTimeIfPresent(ResultSet rs, ResultSetMetaData meta, String column) throws SQLException {
        if (!hasColumn(meta, column)) {
            return null;
        }

        Object raw = rs.getObject(column);
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDateTime ldt) {
            return normalizeDateTime(ldt);
        }
        if (raw instanceof OffsetDateTime odt) {
            return normalizeDateTime(odt.toLocalDateTime());
        }
        if (raw instanceof Timestamp ts) {
            return normalizeDateTime(ts.toLocalDateTime());
        }
        if (raw instanceof String s) {
            return normalizeDateTime(parseDbDate(s));
        }

        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : normalizeDateTime(ts.toLocalDateTime());
    }

    private LocalDateTime parseDbDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        try {
            if (dateStr.matches("^\\d+$")) {
                return normalizeDateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(dateStr)), ZoneId.systemDefault()));
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
}
