package com.app.infrastructure.adapter.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class JdbcAddressRepository {

    private final DatabaseConfig databaseConfig;

    public JdbcAddressRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public Map<String, Integer> countUsersPerNeighbourhood() {
        Map<String, Integer> result = new HashMap<>();
        String query = "SELECT n.name, COUNT(DISTINCT a.user_id) AS user_count " +
                "FROM addresses a " +
                "JOIN neighbourhoods n ON a.neighbourhood_id = n.id " +
                "WHERE a.user_id IS NOT NULL " +
                "GROUP BY n.name";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.put(rs.getString("name"), rs.getInt("user_count"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public Map<String, Integer> countAddressesPerCountry() {
        Map<String, Integer> result = new HashMap<>();
        String query = "SELECT country_code, COUNT(*) AS address_count " +
                "FROM addresses " +
                "GROUP BY country_code";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.put(rs.getString("country_code"), rs.getInt("address_count"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}