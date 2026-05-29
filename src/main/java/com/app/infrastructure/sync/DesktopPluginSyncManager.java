package com.app.infrastructure.sync;

import java.util.List;

import com.app.domain.model.PluginMetadata;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.util.DailyLogger;

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

    public void syncWithBackend() {
        try {
            List<PluginMetadata> plugins = backendGateway.fetchDesktopPlugins();
            sqliteGateway.replaceDesktopPlugins(plugins);
            DailyLogger.logInfo("Sync", "Desktop plugin catalog synchronized: " + plugins.size());
        } catch (Exception e) {
            DailyLogger.logError("Sync", "Desktop plugin sync failed: " + e.getMessage(), e);
        }
    }
}