package com.app.domain.service;

import com.app.domain.model.EventPlanning;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.EventPlanningSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventPlanningService {

    private final EventPlanningSqliteGateway sqliteGateway;

    public EventPlanningService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new EventPlanningSqliteGateway(databaseConfig);
    }

    public List<EventPlanning> getAllEventPlannings() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, EventPlanning> map = sqliteGateway.loadEventPlanningsById(connection);
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateEventPlanning(EventPlanning eventPlanning) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertEventPlanning(connection, eventPlanning);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}