package com.app.infrastructure.sync;

import com.app.domain.model.EventParticipation;
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

public class EventParticipationBackendGateway {

    private final ConfigProvider configProvider;

    public EventParticipationBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = Objects.requireNonNull(configProvider);
    }

    public Map<String, EventParticipation> fetchEventParticipationsById() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) return new HashMap<>();

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        Map<String, EventParticipation> byId = new HashMap<>();
        String sql = "SELECT id::text AS id, subscribed_at, status, signature_url, \"rejectedReason\", user_id, event_id FROM \"event-participation\"";

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                EventParticipation ep = new EventParticipation(
                        rs.getString("id"),
                        rs.getTimestamp("subscribed_at") != null ? rs.getTimestamp("subscribed_at").toLocalDateTime() : null,
                        rs.getString("status"),
                        rs.getString("signature_url"),
                        rs.getString("rejectedReason"),
                        rs.getString("user_id"),
                        rs.getString("event_id"),
                        null,
                        null
                );
                byId.put(ep.id(), ep);
            }
            DailyLogger.logInfo("Sync", "Fetched " + byId.size() + " event participations from PostgreSQL.");
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Failed to fetch event participations: " + e.getMessage(), e);
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