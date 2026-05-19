package com.app.infrastructure.sync;

import com.app.domain.model.Neighbourhood;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.util.DailyLogger;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NeighbourhoodBackendGateway {

    private final ConfigProvider configProvider;
    private final AuthenticatedHttpClient authenticatedHttpClient;

    public NeighbourhoodBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = Objects.requireNonNull(configProvider);
        this.authenticatedHttpClient = Objects.requireNonNull(authenticatedHttpClient);
    }

    public Map<String, Neighbourhood> fetchNeighbourhoodsById() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            DailyLogger.logWarn("Sync", "app.sync.db.url is missing. Cannot fetch neighbourhoods.");
            return new HashMap<>();
        }

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        Map<String, Neighbourhood> byId = new HashMap<>();

        // On caste le polygon en texte pour SQLite
        String sql = "SELECT id, name, description, city, postal_code, country_code, estimated_population, polygon::text as polygon, area FROM neighbourhood";

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Neighbourhood neighbourhood = new Neighbourhood(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("city"),
                        rs.getString("postal_code"),
                        rs.getString("country_code"),
                        rs.getObject("estimated_population") != null ? rs.getInt("estimated_population") : null,
                        rs.getString("polygon"),
                        rs.getObject("area") != null ? rs.getInt("area") : null,
                        null, // Pas de last_modified natif
                        null  // Pas de sync_status distant
                );
                byId.put(neighbourhood.id(), neighbourhood);
            }
            DailyLogger.logInfo("Sync", "Fetched " + byId.size() + " neighbourhoods from PostgreSQL.");
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Failed to fetch neighbourhoods from PostgreSQL: " + e.getMessage(), e);
        }

        return byId;
    }

    private String toJdbcPostgresUrl(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) return trimmed;

        if (trimmed.startsWith("postgresql://") || trimmed.startsWith("postgres://")) {
            String normalized = trimmed.startsWith("postgres://")
                    ? "postgresql://" + trimmed.substring("postgres://".length())
                    : trimmed;

            URI uri = URI.create(normalized);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String db = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                    .append(host).append(":").append(port).append("/").append(db);

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

            if (params.length() > 0) jdbc.append("?").append(params);
            return jdbc.toString();
        }
        return trimmed;
    }
}