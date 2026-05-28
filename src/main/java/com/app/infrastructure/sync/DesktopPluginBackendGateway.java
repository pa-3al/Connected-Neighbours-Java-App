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
}