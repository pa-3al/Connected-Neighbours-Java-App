package com.app.infrastructure.sync;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.app.domain.model.PluginMetadata;
import com.app.domain.model.PluginOrigin;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.util.DailyLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DesktopPluginBackendGateway {

    private final ConfigProvider configProvider;
    private final AuthenticatedHttpClient authenticatedHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DesktopPluginBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = Objects.requireNonNull(configProvider);
        this.authenticatedHttpClient = Objects.requireNonNull(authenticatedHttpClient);
    }

    public List<PluginMetadata> fetchDesktopPlugins() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            return List.of();
        }

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        List<PluginMetadata> plugins = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM desktop_plugins")) {

            while (rs.next()) {
                String id = readString(rs, "id", null);
                String pluginName = readString(rs, "plugin_name", null);
                String pluginversion = readString(rs, "version", null);
                String plugingDescription = readString(rs, "description", null);
                boolean pluginEnabled = readBoolean(rs, "active", true);
                String pluginAuthor = readString(rs, "author", "Voisinea");
                if (id == null || id.isBlank()) {
                    continue;
                }

                plugins.add(new PluginMetadata(
                        id,
                        pluginName,
                        pluginversion,
                        pluginAuthor,
                        plugingDescription,
                        pluginEnabled,
                        false,
                        null,
                        fetchPluginDownloadUrl(id),
                        PluginOrigin.REMOTE_CATALOG
                ));
            }
            DailyLogger.logInfo("Sync", "Fetched " + plugins.size() + " desktop plugins from PostgreSQL.");
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Failed to fetch desktop plugins: " + e.getMessage(), e);
        }

        return plugins;
    }

    public String fetchPluginDownloadUrl(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return null;
        }

        try {
            String url = configProvider.getAuthBaseUrl() + "/admin/desktop/plugins/" + URLEncoder.encode(pluginId, StandardCharsets.UTF_8);
            String body = authenticatedHttpClient.send(java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build()).body();
            return extractDownloadUrl(body);
        } catch (Exception e) {
            DailyLogger.logWarn("Sync", "Failed to resolve desktop plugin download URL for " + pluginId + ": " + e.getMessage());
            return null;
        }
    }

    private String extractDownloadUrl(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        String trimmed = body.trim();
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            JsonNode downloadUrlNode = node.get("jarDownloadUrl");
            if (downloadUrlNode != null && !downloadUrlNode.isNull()) {
                return downloadUrlNode.asText();
            }
        } catch (JsonProcessingException e) {
            DailyLogger.logWarn("Sync", "Failed to resolve desktop plugin download URL : " + e.getMessage());
            return null;
        }
        return trimmed;
    }

    private String toJdbcPostgresUrl(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return trimmed;
        }
        if (trimmed.startsWith("postgresql://") || trimmed.startsWith("postgres://")) {
            String normalized = trimmed.startsWith("postgres://") ? "postgresql://" + trimmed.substring(11) : trimmed;
            URI uri = URI.create(normalized);
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost()).append(":").append(uri.getPort() == -1 ? 5432 : uri.getPort()).append("/").append(uri.getPath().replaceFirst("^/", ""));
            
            boolean hasQuery = false;
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                jdbc.append("?").append(uri.getRawQuery());
                hasQuery = true;
            }
            
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                jdbc.append(hasQuery ? "&" : "?")
                    .append("user=").append(parts[0]);
                if (parts.length > 1) {
                    jdbc.append("&password=").append(parts[1]);
                }
            }
            
            return jdbc.toString();
        }
        return trimmed;
    }

    private String readString(ResultSet rs, String column, String defaultValue) {
        try {
            String value = rs.getString(column);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean readBoolean(ResultSet rs, String column, boolean defaultValue) {
        try {
            Object value = rs.getObject(column);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Boolean boolValue) {
                return boolValue;
            }
            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}