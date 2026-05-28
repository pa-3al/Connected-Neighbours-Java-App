package com.app.infrastructure.sync;

import com.app.domain.service.*;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.ui.AppState;
import com.app.infrastructure.util.ConnectivityUtil;
import com.app.infrastructure.util.DailyLogger;

import javafx.application.Platform;

public class BackgroundSyncManager {

    private final UserService userService;
    private final IncidentSyncManager incidentSyncManager;
    private final AddressSyncManager addressSyncManager;
    private final NeighbourhoodSyncManager neighbourhoodSyncManager;
    private final CategorySyncManager categorySyncManager;
    private final ContractTemplateSyncManager contractTemplateSyncManager;
    private final MediaSyncManager mediaSyncManager;
    private final EventSyncManager eventSyncManager;
    private final EventTagSyncManager eventTagSyncManager;
    private final EventPlanningSyncManager eventPlanningSyncManager;
    private final EventParticipationSyncManager eventParticipationSyncManager;
    private final ServiceSyncManager serviceSyncManager;
    private final ServiceExpectedDateSyncManager serviceExpectedDateSyncManager;
    private final DesktopPluginSyncManager desktopPluginSyncManager;

    public BackgroundSyncManager(
            UserService userService,
            IncidentService incidentService,
            AddressService addressService,
            NeighbourhoodService neighbourhoodService,
            CategoryService categoryService,
            ContractTemplateService contractTemplateService,
            MediaService mediaService,
            EventService eventService,
            EventTagService eventTagService,
            EventPlanningService eventPlanningService,
            EventParticipationService eventParticipationService,
            ServiceService serviceService,
            ServiceExpectedDateService serviceExpectedDateService
    ) {
        this.userService = userService;
        ConfigProvider config = new ConfigProvider();
        AuthenticatedHttpClient authClient = new AuthenticatedHttpClient();

        this.incidentSyncManager = new IncidentSyncManager(incidentService);
        this.addressSyncManager = new AddressSyncManager(addressService);
        this.neighbourhoodSyncManager = new NeighbourhoodSyncManager(neighbourhoodService);
        this.categorySyncManager = new CategorySyncManager(categoryService, config, authClient);
        this.contractTemplateSyncManager = new ContractTemplateSyncManager(contractTemplateService, config, authClient);
        this.mediaSyncManager = new MediaSyncManager(mediaService, config, authClient);
        this.eventSyncManager = new EventSyncManager(eventService, config, authClient);
        this.eventTagSyncManager = new EventTagSyncManager(eventTagService, config, authClient);
        this.eventPlanningSyncManager = new EventPlanningSyncManager(eventPlanningService, config, authClient);
        this.eventParticipationSyncManager = new EventParticipationSyncManager(eventParticipationService, config, authClient);
        this.serviceSyncManager = new ServiceSyncManager(serviceService);
        this.serviceExpectedDateSyncManager = new ServiceExpectedDateSyncManager(serviceExpectedDateService);
        this.desktopPluginSyncManager = new DesktopPluginSyncManager(new com.app.infrastructure.adapter.persistence.DatabaseConfig(), config, authClient);
    }

    public void startAutomaticSyncOnStartup() {
        Thread syncThread = new Thread(() -> {
            while (true) {
                try {
                    if (ConnectivityUtil.checkConnectivity()) {
                        Platform.runLater(() -> AppState.getInstance().setSyncing(true));
                        try {
                            userService.syncUsers();
                            neighbourhoodSyncManager.syncWithBackend(conflict -> conflict.server());
                            addressSyncManager.syncWithBackend(conflict -> conflict.server());
                            categorySyncManager.syncWithBackend(conflict -> conflict.server());
                            contractTemplateSyncManager.syncWithBackend(conflict -> conflict.server());
                            mediaSyncManager.syncWithBackend(conflict -> conflict.server());
                            eventTagSyncManager.syncWithBackend(conflict -> conflict.server());
                            eventSyncManager.syncWithBackend(conflict -> conflict.server());
                            eventPlanningSyncManager.syncWithBackend(conflict -> conflict.server());
                            eventParticipationSyncManager.syncWithBackend(conflict -> conflict.server());
                            incidentSyncManager.syncWithBackend(conflict -> conflict.serverIncident());
                            serviceSyncManager.syncWithBackend(conflict -> conflict.server());
                            serviceExpectedDateSyncManager.syncWithBackend(conflict -> conflict.server());
                            desktopPluginSyncManager.syncWithBackend();
                        } catch (Exception dbError) {
                            DailyLogger.logError("Sync", "Erreur BDD au démarrage, synchro ignorée: " + dbError.getMessage(), dbError);
                        } finally {
                            Platform.runLater(() -> AppState.getInstance().setSyncing(false));
                        }
                        break;
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    DailyLogger.logError("Sync", "Erreur inattendue: " + e.getMessage(), e);
                    break;
                }
            }
        });
        syncThread.setDaemon(true);
        syncThread.start();
    }
}
