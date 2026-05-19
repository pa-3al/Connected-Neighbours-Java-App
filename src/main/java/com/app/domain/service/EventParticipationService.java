package com.app.domain.service;

import com.app.domain.model.EventParticipation;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.EventParticipationSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventParticipationService {

    private final EventParticipationSqliteGateway sqliteGateway;

    public EventParticipationService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new EventParticipationSqliteGateway(databaseConfig);
    }

    public List<EventParticipation> getAllEventParticipations() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, EventParticipation> map = sqliteGateway.loadEventParticipationsById(connection);
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateEventParticipation(EventParticipation eventParticipation) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertEventParticipation(connection, eventParticipation);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}