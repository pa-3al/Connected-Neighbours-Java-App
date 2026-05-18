package com.app.infrastructure.sync;

import com.app.domain.model.Address;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddressSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public AddressSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, Address> loadAddressesById(Connection connection) throws SQLException {
        Map<String, Address> map = new HashMap<>();
        String sql = "SELECT * FROM addresses";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new Address(
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
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertAddress(Connection connection, Address a) throws SQLException {
        String sql = """
            INSERT INTO addresses (id, street_number, address_line_2, street_name, city, postal_code, region, country_code, location, active, neighbourhood_id, user_id, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                street_number=excluded.street_number, address_line_2=excluded.address_line_2, 
                street_name=excluded.street_name, city=excluded.city, postal_code=excluded.postal_code, 
                region=excluded.region, country_code=excluded.country_code, location=excluded.location, 
                active=excluded.active, neighbourhood_id=excluded.neighbourhood_id, user_id=excluded.user_id, 
                last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, a.id());
            pstmt.setString(2, a.streetNumber());
            pstmt.setString(3, a.addressLine2());
            pstmt.setString(4, a.streetName());
            pstmt.setString(5, a.city());
            pstmt.setString(6, a.postalCode());
            pstmt.setString(7, a.region());
            pstmt.setString(8, a.countryCode());
            pstmt.setString(9, a.location());
            pstmt.setBoolean(10, a.active() != null ? a.active() : true);
            pstmt.setString(11, a.neighbourhoodId());
            pstmt.setString(12, a.userId());
            pstmt.setTimestamp(13, a.lastModified() != null ? Timestamp.valueOf(a.lastModified()) : null);
            pstmt.setString(14, a.syncStatus() != null ? a.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}