package com.app.infrastructure.sync;

import com.app.domain.model.Service;
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

public class ServiceBackendGateway {

    private final ConfigProvider configProvider;
    private final AuthenticatedHttpClient authenticatedHttpClient;

    public ServiceBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = Objects.requireNonNull(configProvider);
        this.authenticatedHttpClient = Objects.requireNonNull(authenticatedHttpClient);
    }

    public Map<String, Service> fetchServicesById() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            return new HashMap<>();
        }

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        Map<String, Service> byId = new HashMap<>();

        String sql = "SELECT id, type, title, description, points, status, moderator_comment, signature_url, contract_id, service_type_id, address_id, created_by_user_id, approved_by_moderator_id, created_at, updated_at FROM services";

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Service service = new Service(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("points"),
                        rs.getString("status"),
                        rs.getString("moderator_comment"),
                        rs.getString("signature_url"),
                        rs.getString("contract_id"),
                        rs.getString("service_type_id"),
                        rs.getString("address_id"),
                        rs.getString("created_by_user_id"),
                        rs.getString("approved_by_moderator_id"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        null,
                        null
                );
                byId.put(service.id(), service);
            }
            DailyLogger.logInfo("Sync", "Fetched " + byId.size() + " services from PostgreSQL.");
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Failed to fetch services from PostgreSQL: " + e.getMessage(), e);
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