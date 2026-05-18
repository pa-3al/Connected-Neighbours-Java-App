package com.app.infrastructure.di;

import com.app.domain.port.out.*;
import com.app.domain.service.*;
import com.app.infrastructure.adapter.auth.HttpAdminAuthRepository;
import com.app.infrastructure.adapter.i18n.I18nAdapter;
import com.app.infrastructure.adapter.logging.LoggerAdapter;
import com.app.infrastructure.adapter.persistence.*;
import com.app.infrastructure.adapter.plugin.DefaultPluginContext;
import com.app.infrastructure.adapter.plugin.FileSystemPluginAdapter;
import com.app.infrastructure.adapter.plugin.HeadlessPluginContext;
import com.app.infrastructure.adapter.query.JFlexQueryAdapter;
import com.app.infrastructure.adapter.update.HttpUpdateAdapter;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.sync.BackgroundSyncManager;
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
    private volatile HttpAdminAuthRepository authRepository;

    private volatile BackgroundSyncManager backgroundSyncManager;
    private volatile DatabaseConfig databaseConfig;
    private volatile IncidentRepository incidentRepository;
    private volatile AlertRepository alertRepository;
    private volatile UserRepository userRepository;

    private volatile IncidentService incidentService;
    private volatile AlertService alertService;
    private volatile AuthService authService;
    private volatile UserService userService;
    private volatile AddressService addressService;
    private volatile NeighbourhoodService neighbourhoodService;

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

    public UserService getUserService() {
        if (userService == null) {
            synchronized (this) {
                if (userService == null) {
                    RemoteUserRepository remoteUserRepository = new JdbcRemoteUserRepository(configProvider);
                    userService = new UserService(getUserRepository(), remoteUserRepository) {
                    };
                }
            }
        }
        return userService;
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

    public AddressService getAddressService() {
        if (addressService == null) {
            synchronized (this) {
                if (addressService == null) {
                    addressService = new AddressService(getDatabaseConfig());
                }
            }
        }
        return addressService;
    }

    public NeighbourhoodService getNeighbourhoodService() {
        if (neighbourhoodService == null) {
            synchronized (this) {
                if (neighbourhoodService == null) {
                    neighbourhoodService = new NeighbourhoodService(getDatabaseConfig());
                }
            }
        }
        return neighbourhoodService;
    }

    public PluginService getPluginService() {
        if (pluginService == null) {
            synchronized (this) {
                if (pluginService == null) {
                    pluginService = new PluginService(
                            getPluginAdapter(),
                            getPluginContext(),
                            getI18nPort(),
                            getLoggerPort()
                    );
                }
            }
        }
        return pluginService;
    }

    public AuthService getAuthService() {
        if (authService == null) {
            synchronized (this) {
                if (authService == null) {
                    authService = new AuthService(getAuthRepository());
                }
            }
        }
        return authService;
    }

    public boolean isAuthBypassEnabled() {
        return configProvider.isAuthBypassEnabled();
    }

    public String getAuthBypassToken() {
        return configProvider.getAuthBypassToken();
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
                    pluginContext = isHeadless
                            ? new HeadlessPluginContext()
                            : new DefaultPluginContext();
                }
            }
        }
        return pluginContext;
    }

    public BackgroundSyncManager getBackgroundSyncManager() {
        if (backgroundSyncManager == null) {
            synchronized (this) {
                if (backgroundSyncManager == null) {
                    backgroundSyncManager = new BackgroundSyncManager(
                            getUserService(),
                            getIncidentService(),
                            getAddressService(),
                            getNeighbourhoodService()
                    );
                }
            }
        }
        return backgroundSyncManager;
    }

    public HttpUpdateAdapter getUpdateAdapter() {
        if (updateAdapter == null) {
            synchronized (this) {
                if (updateAdapter == null) {
                    updateAdapter = new HttpUpdateAdapter(configProvider, com.app.infrastructure.ui.AppState.getInstance()::isOnline);
                }
            }
        }
        return updateAdapter;
    }

    public HttpAdminAuthRepository getAuthRepository() {
        if (authRepository == null) {
            synchronized (this) {
                if (authRepository == null) {
                    authRepository = new HttpAdminAuthRepository(configProvider);
                }
            }
        }
        return authRepository;
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
                            configProvider.getMaxPluginSizeBytes()
                    );
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

    private UserRepository getUserRepository() {
        if (userRepository == null) {
            synchronized (this) {
                if (userRepository == null) {
                    userRepository = new JdbcUserRepository(getDatabaseConfig());
                }
            }
        }
        return userRepository;
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