package com.app.domain.service;

import com.app.domain.model.Event;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.EventSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventService {

    private final EventSqliteGateway sqliteGateway;

    public EventService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new EventSqliteGateway(databaseConfig);
    }

    public List<Event> getAllEvents() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, Event> map = sqliteGateway.loadEventsById(connection);
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateEvent(Event event) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertEvent(connection, event);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}