package com.app.domain.service;

import com.app.domain.model.Media;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.MediaSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaService {

    private final MediaSqliteGateway sqliteGateway;

    public MediaService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new MediaSqliteGateway(databaseConfig);
    }

    public List<Media> getAllMedia() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, Media> map = sqliteGateway.loadMediaById(connection);
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateMedia(Media media) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertMedia(connection, media);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}