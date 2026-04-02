package com.app.infrastructure.adapter.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {

    private final DatabaseConfig databaseConfig;

    public SchemaInitializer(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void initialize() {
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(36) PRIMARY KEY,
                    email VARCHAR(255),
                    firstname VARCHAR(255),
                    lastname VARCHAR(255),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);
            
            // Incident
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS incidents (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description CLOB,
                    category VARCHAR(50),
                    status VARCHAR(50),
                    priority VARCHAR(50),
                    reported_by_user_id VARCHAR(36),
                    reported_by VARCHAR(255),
                    location VARCHAR(255),
                    reported_at TIMESTAMP,
                    resolved_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            addColumnIfMissing(conn, "incidents", "reported_by_user_id", "VARCHAR(36)");
            addColumnIfMissing(conn, "users", "last_modified", "TIMESTAMP");
            addColumnIfMissing(conn, "users", "sync_status", "VARCHAR(50)");

            // Alerte
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS alerts (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    message CLOB,
                    severity VARCHAR(50),
                    created_at TIMESTAMP,
                    expires_at TIMESTAMP,
                    is_active BOOLEAN,
                    created_by VARCHAR(255),
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private void addColumnIfMissing(Connection conn, String tableName, String columnName, String definition)
            throws SQLException {
        if (hasColumn(conn, tableName, columnName)) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
