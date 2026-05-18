package com.app.infrastructure.sync;

import com.app.domain.model.Neighbourhood;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NeighbourhoodSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public NeighbourhoodSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, Neighbourhood> loadNeighbourhoodsById(Connection connection) throws SQLException {
        Map<String, Neighbourhood> map = new HashMap<>();
        String sql = "SELECT * FROM neighbourhoods";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new Neighbourhood(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("city"),
                        rs.getString("postal_code"),
                        rs.getString("country_code"),
                        rs.getObject("estimated_population") != null ? rs.getInt("estimated_population") : null,
                        rs.getString("polygon"),
                        rs.getObject("area") != null ? rs.getInt("area") : null,
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertNeighbourhood(Connection connection, Neighbourhood n) throws SQLException {
        String sql = """
            INSERT INTO neighbourhoods (id, name, description, city, postal_code, country_code, estimated_population, polygon, area, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                name=excluded.name, description=excluded.description, city=excluded.city, 
                postal_code=excluded.postal_code, country_code=excluded.country_code, 
                estimated_population=excluded.estimated_population, polygon=excluded.polygon, 
                area=excluded.area, last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, n.id());
            pstmt.setString(2, n.name());
            pstmt.setString(3, n.description());
            pstmt.setString(4, n.city());
            pstmt.setString(5, n.postalCode());
            pstmt.setString(6, n.countryCode());

            if (n.estimatedPopulation() != null) pstmt.setInt(7, n.estimatedPopulation());
            else pstmt.setNull(7, Types.INTEGER);

            pstmt.setString(8, n.polygon());

            if (n.area() != null) pstmt.setInt(9, n.area());
            else pstmt.setNull(9, Types.INTEGER);

            pstmt.setTimestamp(10, n.lastModified() != null ? Timestamp.valueOf(n.lastModified()) : null);
            pstmt.setString(11, n.syncStatus() != null ? n.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}