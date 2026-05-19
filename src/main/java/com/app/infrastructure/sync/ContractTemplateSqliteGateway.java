package com.app.infrastructure.sync;

import com.app.domain.model.ContractTemplate;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ContractTemplateSqliteGateway {

    private final DatabaseConfig databaseConfig;

    public ContractTemplateSqliteGateway(DatabaseConfig databaseConfig) {
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
    }

    public Connection openLocalConnection() throws SQLException {
        return databaseConfig.getConnection();
    }

    public Map<String, ContractTemplate> loadContractTemplatesById(Connection connection) throws SQLException {
        Map<String, ContractTemplate> map = new HashMap<>();
        String sql = "SELECT * FROM contract_templates";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("id"), new ContractTemplate(
                        rs.getString("id"),
                        rs.getString("contract_type"),
                        rs.getString("language_id"),
                        rs.getString("document_path"),
                        rs.getString("original_file_name"),
                        rs.getString("file_extension"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        rs.getTimestamp("last_modified") != null ? rs.getTimestamp("last_modified").toLocalDateTime() : null,
                        rs.getString("sync_status") != null ? SyncStatus.valueOf(rs.getString("sync_status")) : null
                ));
            }
        }
        return map;
    }

    public void upsertContractTemplate(Connection connection, ContractTemplate ct) throws SQLException {
        String sql = """
            INSERT INTO contract_templates (id, contract_type, language_id, document_path, original_file_name, file_extension, active, created_at, updated_at, last_modified, sync_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                contract_type=excluded.contract_type, language_id=excluded.language_id, document_path=excluded.document_path, 
                original_file_name=excluded.original_file_name, file_extension=excluded.file_extension, active=excluded.active, 
                created_at=excluded.created_at, updated_at=excluded.updated_at, last_modified=excluded.last_modified, sync_status=excluded.sync_status
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ct.id());
            pstmt.setString(2, ct.contractType());
            pstmt.setString(3, ct.languageId());
            pstmt.setString(4, ct.documentPath());
            pstmt.setString(5, ct.originalFileName());
            pstmt.setString(6, ct.fileExtension());
            pstmt.setBoolean(7, ct.active() != null ? ct.active() : true);
            pstmt.setTimestamp(8, ct.createdAt() != null ? Timestamp.valueOf(ct.createdAt()) : null);
            pstmt.setTimestamp(9, ct.updatedAt() != null ? Timestamp.valueOf(ct.updatedAt()) : null);
            pstmt.setTimestamp(10, ct.lastModified() != null ? Timestamp.valueOf(ct.lastModified()) : null);
            pstmt.setString(11, ct.syncStatus() != null ? ct.syncStatus().name() : null);
            pstmt.executeUpdate();
        }
    }
}