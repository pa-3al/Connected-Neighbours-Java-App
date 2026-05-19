package com.app.infrastructure.sync;

import com.app.domain.model.Address;
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

public class AddressBackendGateway {

    private final ConfigProvider configProvider;
    private final AuthenticatedHttpClient authenticatedHttpClient;

    public AddressBackendGateway(ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.configProvider = Objects.requireNonNull(configProvider);
        this.authenticatedHttpClient = Objects.requireNonNull(authenticatedHttpClient);
    }

    public Map<String, Address> fetchAddressesById() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            DailyLogger.logWarn("Sync", "app.sync.db.url is missing. Cannot fetch addresses.");
            return new HashMap<>();
        }

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        Map<String, Address> byId = new HashMap<>();

        // On caste location en text pour que le JDBC driver PostgreSQL le passe en chaîne de caractères à SQLite
        String sql = "SELECT id, street_number, address_line_2, street_name, city, postal_code, region, country_code, location::text as location, active, neighbourhood_id, user_id FROM address";

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Address address = new Address(
                        rs.getString("id"),
                        rs.getString("street_number"),
                        rs.getString("address_line_2"),
                        rs.getString("street_name"),
                        rs.getString("city"),
                        rs.getString("postal_code"),
                        rs.getString("region"),
                        rs.getString("country_code"),
                        rs.getString("location"),
                        rs.getBoolean("active"),
                        rs.getString("neighbourhood_id"),
                        rs.getString("user_id"),
                        null, // PostgreSQL local n'a pas last_modified dans ton entity actuelle
                        null  // Pas de sync_status distant non plus
                );
                byId.put(address.id(), address);
            }
            DailyLogger.logInfo("Sync", "Fetched " + byId.size() + " addresses from PostgreSQL.");
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Failed to fetch addresses from PostgreSQL: " + e.getMessage(), e);
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