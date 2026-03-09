package com.app.infrastructure.adapter.persistence;

import java.sql.Connection;
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
            
            // Incident
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS incidents (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description CLOB,
                    category VARCHAR(50),
                    status VARCHAR(50),
                    priority VARCHAR(50),
                    reported_by VARCHAR(255),
                    location VARCHAR(255),
                    reported_at TIMESTAMP,
                    resolved_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

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
}
