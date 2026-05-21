package com.app.infrastructure.adapter.persistence;

import com.app.domain.port.out.ServiceRepository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class JdbcServiceRepository implements ServiceRepository {

    private final DatabaseConfig databaseConfig;

    public JdbcServiceRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public Map<String, Integer> countServicesByStatus() {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as count FROM services GROUP BY status";

        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                result.put(status == null ? "UNKNOWN" : status, count);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public Map<String, Integer> countExpectedDatesByMonth() {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT start_date FROM service_expected_dates WHERE start_date IS NOT NULL";

        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String dateStr = rs.getString("start_date");
                LocalDateTime date = parseDbDate(dateStr);
                if (date != null) {
                    String month = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                    result.put(month, result.getOrDefault(month, 0) + 1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private LocalDateTime parseDbDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            if (dateStr.matches("^\\d+$")) {
                return LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(Long.parseLong(dateStr)),
                        java.time.ZoneId.systemDefault()
                );
            }
            String normalized = dateStr.replace(' ', 'T');
            return LocalDateTime.parse(normalized);
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}