package com.app.infrastructure.adapter.persistence;

import com.app.domain.port.out.EventStatsRepository;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JdbcEventStatsRepository implements EventStatsRepository {

    private final DatabaseConfig dbConfig;

    public JdbcEventStatsRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public Map<String, Integer> getEventsByMonth() {
        Map<String, Integer> result = new LinkedHashMap<>();

        String query = """
            SELECT strftime('%Y-%m', last_modified) as month, COUNT(id) as count
            FROM events
            GROUP BY month
            ORDER BY month
        """;

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                result.put(rs.getString("month"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public Map<String, Integer> getTopEventsByParticipation() {
        Map<String, Integer> result = new LinkedHashMap<>();

        String query = """
            SELECT e.name, COUNT(ep.user_id) as participants
            FROM events e
            LEFT JOIN event_participations ep ON e.id = ep.event_id
            GROUP BY e.id
            ORDER BY participants DESC
            LIMIT 5
        """;

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                result.put(rs.getString("name"), rs.getInt("participants"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public Map<String, Integer> getEventsByPriceType() {
        Map<String, Integer> result = new LinkedHashMap<>();

        String query = """
            SELECT
                CASE WHEN real_money_price > 0 THEN 'Payant' ELSE 'Gratuit' END as type,
                COUNT(*) as count
            FROM events
            GROUP BY type
        """;

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                result.put(rs.getString("type"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public Map<String, Integer> getEventPriceDistribution() {
        Map<String, Integer> result = new LinkedHashMap<>();

        String query = """
        SELECT
            CASE
                WHEN real_money_price = 0 THEN 'Gratuit (0€)'
                WHEN real_money_price > 0 AND real_money_price < 5 THEN 'Faible (<5€)'
                WHEN real_money_price >= 5 AND real_money_price < 15 THEN 'Moyen (5-15€)'
                ELSE 'Élevé (≥15€)'
            END as range,
            COUNT(*) as count
        FROM events
        GROUP BY range
        ORDER BY
            CASE range
                WHEN 'Gratuit (0€)' THEN 1
                WHEN 'Faible (<5€)' THEN 2
                WHEN 'Moyen (5-15€)' THEN 3
                WHEN 'Élevé (≥15€)' THEN 4
            END
    """;

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                result.put(rs.getString("range"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public Map<String, Double> getAveragePriceByMonth() {
        Map<String, Double> result = new LinkedHashMap<>();

        String query = """
            SELECT
                strftime('%Y-%m', last_modified) as month,
                AVG(real_money_price) as avg_price
            FROM events
            GROUP BY month
            ORDER BY month
        """;

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                result.put(rs.getString("month"), rs.getDouble("avg_price"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}