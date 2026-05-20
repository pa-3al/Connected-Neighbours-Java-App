package com.app.infrastructure.adapter.persistence;

import com.app.domain.model.Neighbourhood;
import com.app.domain.model.SyncStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class JdbcNeighbourhoodRepository {

    private final DatabaseConfig databaseConfig;

    public JdbcNeighbourhoodRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public List<Neighbourhood> findAll() {
        List<Neighbourhood> neighbourhoods = new ArrayList<>();
        String sql = "SELECT id, name, description, city, postal_code, country_code, estimated_population, polygon, area, last_modified, sync_status FROM neighbourhoods";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                SyncStatus syncStatus = null;
                String statusStr = rs.getString("sync_status");
                if (statusStr != null) {
                    try {
                        syncStatus = SyncStatus.valueOf(statusStr);
                    } catch (IllegalArgumentException e) {
                    }
                }

                int populationRaw = rs.getInt("estimated_population");
                Integer estimatedPopulation = rs.wasNull() ? null : populationRaw;

                int areaRaw = rs.getInt("area");
                Integer area = rs.wasNull() ? null : areaRaw;

                LocalDateTime lastModified = null;
                long lastModifiedLong = rs.getLong("last_modified");
                if (!rs.wasNull() && lastModifiedLong > 0) {
                    try {
                        lastModified = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(lastModifiedLong),
                                ZoneId.systemDefault()
                        );
                    } catch (Exception e) {
                        System.err.println("[WARN] Erreur lors de la conversion du timestamp millisecondes : " + lastModifiedLong);
                    }
                }

                Neighbourhood neighbourhood = new Neighbourhood(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("city"),
                        rs.getString("postal_code"),
                        rs.getString("country_code"),
                        estimatedPopulation,
                        rs.getString("polygon"),
                        area,
                        lastModified,
                        syncStatus
                );
                neighbourhoods.add(neighbourhood);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching neighbourhoods from database", e);
        }
        return neighbourhoods;
    }
}