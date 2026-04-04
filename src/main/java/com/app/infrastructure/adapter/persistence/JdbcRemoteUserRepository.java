package com.app.infrastructure.adapter.persistence;

import com.app.domain.model.User;
import com.app.domain.port.out.RemoteUserRepository;
import com.app.infrastructure.config.ConfigProvider;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcRemoteUserRepository implements RemoteUserRepository {

    private final ConfigProvider configProvider;

    public JdbcRemoteUserRepository(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public List<User> fetchAllUsers() {
        String rawUrl = configProvider.getSyncDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            return List.of();
        }

        String jdbcUrl = toJdbcPostgresUrl(rawUrl);
        List<User> users = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, firstname, lastname, email FROM users")) {

            while (rs.next()) {
                users.add(new User(
                        rs.getString("id"),
                        rs.getString("firstname"),
                        rs.getString("lastname"),
                        rs.getString("email")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
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

            if (params.length() > 0) {
                jdbc.append("?").append(params);
            }
            return jdbc.toString();
        }
        return trimmed;
    }
}