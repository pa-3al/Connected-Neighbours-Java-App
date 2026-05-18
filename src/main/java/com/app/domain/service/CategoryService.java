package com.app.domain.service;

import com.app.domain.model.Category;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.CategorySqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryService {

    private final CategorySqliteGateway sqliteGateway;

    public CategoryService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new CategorySqliteGateway(databaseConfig);
    }

    public List<Category> getAllCategories() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, Category> map = sqliteGateway.loadCategoriesById(connection);
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCategory(Category category) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertCategory(connection, category);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}