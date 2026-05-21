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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reports (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description CLOB,
                    category VARCHAR(50),
                    status VARCHAR(50),
                    priority VARCHAR(50),
                    reported_by_user_id VARCHAR(36),
                    reported_by VARCHAR(255),
                    location VARCHAR(255),
                    admin_response_message CLOB,
                    reported_at TIMESTAMP,
                    resolved_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS neighbourhoods (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    city VARCHAR(255) NOT NULL,
                    postal_code VARCHAR(20),
                    country_code VARCHAR(2) NOT NULL,
                    estimated_population INTEGER,
                    polygon TEXT,
                    area INTEGER,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS addresses (
                    id VARCHAR(36) PRIMARY KEY,
                    street_number VARCHAR(20) NOT NULL,
                    address_line_2 VARCHAR(255),
                    street_name VARCHAR(255) NOT NULL,
                    city VARCHAR(255) NOT NULL,
                    postal_code VARCHAR(20) NOT NULL,
                    region VARCHAR(255),
                    country_code VARCHAR(5) NOT NULL,
                    location TEXT,
                    active BOOLEAN DEFAULT 1,
                    neighbourhood_id VARCHAR(36),
                    user_id VARCHAR(36),
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50),
                    FOREIGN KEY (neighbourhood_id) REFERENCES neighbourhoods(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(120),
                    type VARCHAR(50),
                    active BOOLEAN DEFAULT 1,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    event_id VARCHAR(36),
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS contract_templates (
                    id VARCHAR(36) PRIMARY KEY,
                    contract_type VARCHAR(20),
                    language_id VARCHAR(36),
                    document_path VARCHAR(512),
                    original_file_name VARCHAR(255),
                    file_extension VARCHAR(10),
                    active BOOLEAN DEFAULT 1,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS media (
                    id VARCHAR(36) PRIMARY KEY,
                    type VARCHAR(50),
                    url VARCHAR(255),
                    file_extension VARCHAR(10),
                    neighbourhood_id VARCHAR(36),
                    event_id VARCHAR(36),
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS event_tags (
                    name VARCHAR(50) PRIMARY KEY,
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description VARCHAR(2048) NOT NULL,
                    points INTEGER DEFAULT 1,
                    real_money_price REAL DEFAULT 0.0,
                    require_validation BOOLEAN DEFAULT 0,
                    signature_url VARCHAR(255),
                    contract_id VARCHAR(36),
                    address_id VARCHAR(36),
                    created_by_user_id VARCHAR(36),
                    approved_by_moderator_id VARCHAR(36),
                    approved_by_admin_id VARCHAR(36),
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS event_plannings (
                    id VARCHAR(36) PRIMARY KEY,
                    start_date TIMESTAMP,
                    end_date TIMESTAMP,
                    event_id VARCHAR(36),
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS event_participations (
                    id VARCHAR(36) PRIMARY KEY,
                    subscribed_at TIMESTAMP,
                    status VARCHAR(50),
                    signature_url VARCHAR(255),
                    rejected_reason VARCHAR(255),
                    user_id VARCHAR(36),
                    event_id VARCHAR(36),
                    last_modified TIMESTAMP,
                    sync_status VARCHAR(50)
                )
            """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS services (
                id VARCHAR(36) PRIMARY KEY,
                type VARCHAR(50),
                title VARCHAR(255) NOT NULL,
                description VARCHAR(2048) NOT NULL,
                points INTEGER DEFAULT 0,
                status VARCHAR(50) DEFAULT 'PENDING',
                moderator_comment TEXT,
                signature_url VARCHAR(255),
                contract_id VARCHAR(36),
                service_type_id VARCHAR(36),
                address_id VARCHAR(36),
                created_by_user_id VARCHAR(36) NOT NULL,
                approved_by_moderator_id VARCHAR(36),
                created_at TIMESTAMP,
                updated_at TIMESTAMP,
                last_modified TIMESTAMP,
                sync_status VARCHAR(50),
                FOREIGN KEY (contract_id) REFERENCES contract_templates(id),
                FOREIGN KEY (service_type_id) REFERENCES categories(id),
                FOREIGN KEY (address_id) REFERENCES addresses(id),
                FOREIGN KEY (created_by_user_id) REFERENCES users(id)
            )
        """);

            addColumnIfMissing(conn, "reports", "reported_by_user_id", "VARCHAR(36)");
            addColumnIfMissing(conn, "users", "last_modified", "TIMESTAMP");
            addColumnIfMissing(conn, "users", "sync_status", "VARCHAR(50)");

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