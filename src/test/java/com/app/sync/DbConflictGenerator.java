package com.app.sync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DbConflictGenerator {

    public static void main(String[] args) throws Exception {
        String dataDir = "data";
        new java.io.File(dataDir).mkdirs();

        String localDb = "jdbc:sqlite:" + dataDir + "/neighborhood.db";
        String serverDb = "jdbc:sqlite:" + dataDir + "/server.db";

        System.out.println("Generating Local DB: " + localDb);
        setupDb(localDb, "Conflict report", "Description locale", "2026-03-31 10:00:00");

        System.out.println("Generating Server DB: " + serverDb);
        setupDb(serverDb, "Conflit sur Report", "Description serveur", "2026-03-31 10:05:00");

        System.out.println("Done. You can now sync neighborhood.db with server.db to see the conflict.");
    }

    private static void setupDb(String url, String title, String description, String lastModified) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reports (
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
                stmt.execute("DELETE FROM reports WHERE id = 'report-conflit-123'");
            }

            String sql = """
                INSERT INTO reports (id, title, description, category, status, priority, reported_by, location, reported_at, last_modified, sync_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "report-conflit-123");
                pstmt.setString(2, title);
                pstmt.setString(3, description);
                pstmt.setString(4, "SECURITY");
                pstmt.setString(5, "OPEN");
                pstmt.setString(6, "HIGH");
                pstmt.setString(7, "TestUser");
                pstmt.setString(8, "Rue de la Paix");
                pstmt.setString(9, "2026-03-31 09:00:00");
                pstmt.setString(10, lastModified);
                pstmt.setString(11, "SYNCED");
                pstmt.executeUpdate();
            }
        }
    }
}
