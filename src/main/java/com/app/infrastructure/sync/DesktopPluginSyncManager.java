package com.app.infrastructure.sync;

import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;

public class DesktopPluginSyncManager {

    private final DesktopPluginBackendGateway backendGateway;
    private final DesktopPluginSqliteGateway sqliteGateway;

    public DesktopPluginSyncManager() {
        this(new DatabaseConfig(), new ConfigProvider(), new AuthenticatedHttpClient());
    }

    public DesktopPluginSyncManager(DatabaseConfig databaseConfig, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.backendGateway = new DesktopPluginBackendGateway(configProvider, authenticatedHttpClient);
        this.sqliteGateway = new DesktopPluginSqliteGateway(databaseConfig);
    }
}