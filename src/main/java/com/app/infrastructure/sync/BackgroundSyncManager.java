package com.app.infrastructure.sync;

import com.app.domain.service.*;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.util.ConnectivityUtil;
import com.app.infrastructure.util.DailyLogger;

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
            EventParticipationService eventParticipationService
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
    }

    public void startAutomaticSyncOnStartup() {
        Thread syncThread = new Thread(() -> {
            while (true) {
                try {
                    if (ConnectivityUtil.checkConnectivity()) {
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
                        break;
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    DailyLogger.logError("Sync", "Erreur: " + e.getMessage(), e);
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        syncThread.setDaemon(true);
        syncThread.start();
    }
}