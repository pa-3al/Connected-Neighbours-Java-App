package com.app.domain.service;

import com.app.domain.model.EventTag;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.EventTagSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventTagService {

    private final EventTagSqliteGateway sqliteGateway;

    public EventTagService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new EventTagSqliteGateway(databaseConfig);
    }

    public List<EventTag> getAllEventTags() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, EventTag> map = sqliteGateway.loadEventTagsById(connection);
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateEventTag(EventTag eventTag) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertEventTag(connection, eventTag);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}