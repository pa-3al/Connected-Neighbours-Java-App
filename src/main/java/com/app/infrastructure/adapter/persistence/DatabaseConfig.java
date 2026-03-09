package com.app.infrastructure.adapter.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public class DatabaseConfig {

    private static final String DB_URL = "jdbc:h2:./data/neighborhood_db;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sananes";
    private static final String DB_PASSWORD = "sananes";

    public DatabaseConfig() {
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
