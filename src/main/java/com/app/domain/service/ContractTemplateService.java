package com.app.domain.service;

import com.app.domain.model.ContractTemplate;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.ContractTemplateSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContractTemplateService {

    private final ContractTemplateSqliteGateway sqliteGateway;

    public ContractTemplateService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new ContractTemplateSqliteGateway(databaseConfig);
    }

    public List<ContractTemplate> getAllContractTemplates() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, ContractTemplate> map = sqliteGateway.loadContractTemplatesById(connection);
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateContractTemplate(ContractTemplate contractTemplate) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertContractTemplate(connection, contractTemplate);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}