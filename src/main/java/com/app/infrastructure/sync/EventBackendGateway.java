package com.app.infrastructure.sync;

import com.app.domain.model.Event;
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

public class EventBackendGateway {

    private final ConfigProvider configProvider;

    public EventBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = Objects.requireNonNull(configProvider);
    }

    public Map<String, Event> fetchEventsById() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) return new HashMap<>();

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        Map<String, Event> byId = new HashMap<>();
        String sql = "SELECT id::text AS id, name, description, points, real_money_price, require_validation, signature_url, contract_id, address_id, created_by_user_id, approved_by_moderator_id, approved_by_admin_id FROM events";

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Event event = new Event(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getObject("points") != null ? rs.getInt("points") : null,
                        rs.getObject("real_money_price") != null ? rs.getDouble("real_money_price") : null,
                        rs.getBoolean("require_validation"),
                        rs.getString("signature_url"),
                        rs.getString("contract_id"),
                        rs.getString("address_id"),
                        rs.getString("created_by_user_id"),
                        rs.getString("approved_by_moderator_id"),
                        rs.getString("approved_by_admin_id"),
                        null,
                        null
                );
                byId.put(event.id(), event);
            }
            DailyLogger.logInfo("Sync", "Fetched " + byId.size() + " events from PostgreSQL.");
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Failed to fetch events: " + e.getMessage(), e);
        }
        return byId;
    }

    private String toJdbcPostgresUrl(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) return trimmed;
        if (trimmed.startsWith("postgresql://") || trimmed.startsWith("postgres://")) {
            String normalized = trimmed.startsWith("postgres://") ? "postgresql://" + trimmed.substring(11) : trimmed;
            URI uri = URI.create(normalized);
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost()).append(":").append(uri.getPort() == -1 ? 5432 : uri.getPort()).append("/").append(uri.getPath().replaceFirst("^/", ""));
            StringBuilder params = new StringBuilder();
            if (uri.getRawQuery() != null) params.append(uri.getRawQuery());
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    if (params.length() > 0) params.append("&");
                    params.append("user=").append(URLEncoder.encode(parts[0], StandardCharsets.UTF_8));
                }
                if (parts.length > 1 && !parts[1].isBlank()) {
                    if (params.length() > 0) params.append("&");
                    params.append("password=").append(URLEncoder.encode(parts[1], StandardCharsets.UTF_8));
                }
            }
            if (params.length() > 0) jdbc.append("?").append(params);
            return jdbc.toString();
        }
        return trimmed;
    }
}