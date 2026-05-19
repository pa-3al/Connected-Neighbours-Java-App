package com.app.infrastructure.sync;

import com.app.domain.model.Category;
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

public class CategoryBackendGateway {

    private final ConfigProvider configProvider;

    public CategoryBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = Objects.requireNonNull(configProvider);
    }

    public Map<String, Category> fetchCategoriesById() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) return new HashMap<>();

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        Map<String, Category> byId = new HashMap<>();
        // Ajout d'un alias "AS id" au cas où le driver l'oublierait suite au cast
        String sql = "SELECT id::text AS id, name, type, active, created_at, updated_at, event_id FROM categories";

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Category category = new Category(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        rs.getString("event_id"),
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        null
                );
                byId.put(category.id(), category);
            }
            DailyLogger.logInfo("Sync", "Fetched " + byId.size() + " categories from PostgreSQL.");
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Failed to fetch categories: " + e.getMessage(), e);
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