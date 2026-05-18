package com.app.domain.service;

import com.app.domain.model.Neighbourhood;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.NeighbourhoodSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NeighbourhoodService {

    private final NeighbourhoodSqliteGateway sqliteGateway;

    public NeighbourhoodService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new NeighbourhoodSqliteGateway(databaseConfig);
    }

    public List<Neighbourhood> getAllNeighbourhoods() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, Neighbourhood> neighbourhoodMap = sqliteGateway.loadNeighbourhoodsById(connection);
            return new ArrayList<>(neighbourhoodMap.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateNeighbourhood(Neighbourhood neighbourhood) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertNeighbourhood(connection, neighbourhood);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}