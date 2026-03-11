package com.app.infrastructure.di;

import com.app.domain.port.out.AlertRepository;
import com.app.domain.port.out.I18nPort;
import com.app.domain.port.out.IncidentRepository;
import com.app.domain.port.out.LoggerPort;
import com.app.domain.port.out.QueryEngine;
import com.app.domain.service.AlertService;
import com.app.domain.service.IncidentService;
import com.app.domain.service.PluginService;
import com.app.domain.service.UpdateService;
import com.app.infrastructure.adapter.i18n.I18nAdapter;
import com.app.infrastructure.adapter.logging.LoggerAdapter;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.adapter.persistence.JdbcAlertRepository;
import com.app.infrastructure.adapter.persistence.JdbcIncidentRepository;
import com.app.infrastructure.adapter.persistence.SchemaInitializer;
import com.app.infrastructure.adapter.plugin.DefaultPluginContext;
import com.app.infrastructure.adapter.plugin.FileSystemPluginAdapter;
import com.app.infrastructure.adapter.plugin.HeadlessPluginContext;
import com.app.infrastructure.adapter.query.JFlexQueryAdapter;
import com.app.infrastructure.adapter.update.HttpUpdateAdapter;
import com.app.infrastructure.config.ConfigProvider;
import com.app.plugin.PluginContext;

public class ServiceContext {
    private final ConfigProvider configProvider;
    private final boolean isHeadless;

    private volatile PluginService pluginService;
    private volatile UpdateService updateService;
    private volatile QueryEngine queryEngine;
    private volatile PluginContext pluginContext;
    private volatile I18nPort i18nPort;
    private volatile LoggerPort loggerPort;
    private volatile FileSystemPluginAdapter pluginAdapter;
    private volatile HttpUpdateAdapter updateAdapter;

    private volatile DatabaseConfig databaseConfig;
    private volatile IncidentRepository incidentRepository;
    private volatile AlertRepository alertRepository;

    private volatile IncidentService incidentService;
    private volatile AlertService alertService;

    public ServiceContext(boolean isHeadless) {
        this.isHeadless = isHeadless;
        this.configProvider = new ConfigProvider();
    }

    public void initializeDatabase() {
        new SchemaInitializer(getDatabaseConfig()).initialize();
    }

    public IncidentService getIncidentService() {
        if (incidentService == null) {
            synchronized (this) {
                if (incidentService == null) {
                    incidentService = new IncidentService(getIncidentRepository());
                }
            }
        }
        return incidentService;
    }

    public AlertService getAlertService() {
        if (alertService == null) {
            synchronized (this) {
                if (alertService == null) {
                    alertService = new AlertService(getAlertRepository());
                }
            }
        }
        return alertService;
    }

    public PluginService getPluginService() {
        if (pluginService == null) {
            synchronized (this) {
                if (pluginService == null) {
                    pluginService = new PluginService(
                            getPluginAdapter(),
                            getPluginContext(),
                            getI18nPort(),
                            getLoggerPort());
                }
            }
        }
        return pluginService;
    }

    public UpdateService getUpdateService() {
        if (updateService == null) {
            synchronized (this) {
                if (updateService == null) {
                    updateService = new UpdateService(getUpdateAdapter(), getLoggerPort());
                }
            }
        }
        return updateService;
    }

    public QueryEngine getQueryEngine() {
        if (queryEngine == null) {
            synchronized (this) {
                if (queryEngine == null) {
                    queryEngine = new JFlexQueryAdapter();
                }
            }
        }
        return queryEngine;
    }

    public PluginContext getPluginContext() {
        if (pluginContext == null) {
            synchronized (this) {
                if (pluginContext == null) {
                    pluginContext = isHeadless ? new HeadlessPluginContext() : new DefaultPluginContext();
                }
            }
        }
        return pluginContext;
    }

    public HttpUpdateAdapter getUpdateAdapter() {
        if (updateAdapter == null) {
            synchronized (this) {
                if (updateAdapter == null) {
                    updateAdapter = new HttpUpdateAdapter(configProvider, () -> true);
                }
            }
        }
        return updateAdapter;
    }

    private I18nPort getI18nPort() {
        if (i18nPort == null) {
            synchronized (this) {
                if (i18nPort == null) {
                    i18nPort = new I18nAdapter();
                }
            }
        }
        return i18nPort;
    }

    private LoggerPort getLoggerPort() {
        if (loggerPort == null) {
            synchronized (this) {
                if (loggerPort == null) {
                    loggerPort = new LoggerAdapter();
                }
            }
        }
        return loggerPort;
    }

    private FileSystemPluginAdapter getPluginAdapter() {
        if (pluginAdapter == null) {
            synchronized (this) {
                if (pluginAdapter == null) {
                    pluginAdapter = new FileSystemPluginAdapter(
                            configProvider.getPluginsPath(),
                            configProvider.getPluginStatePath(),
                            configProvider.getMaxPluginSizeBytes());
                }
            }
        }
        return pluginAdapter;
    }

    private IncidentRepository getIncidentRepository() {
        if (incidentRepository == null) {
            synchronized (this) {
                if (incidentRepository == null) {
                    incidentRepository = new JdbcIncidentRepository(getDatabaseConfig());
                }
            }
        }
        return incidentRepository;
    }

    private AlertRepository getAlertRepository() {
        if (alertRepository == null) {
            synchronized (this) {
                if (alertRepository == null) {
                    alertRepository = new JdbcAlertRepository(getDatabaseConfig());
                }
            }
        }
        return alertRepository;
    }

    private DatabaseConfig getDatabaseConfig() {
        if (databaseConfig == null) {
            synchronized (this) {
                if (databaseConfig == null) {
                    databaseConfig = new DatabaseConfig();
                }
            }
        }
        return databaseConfig;
    }
}
