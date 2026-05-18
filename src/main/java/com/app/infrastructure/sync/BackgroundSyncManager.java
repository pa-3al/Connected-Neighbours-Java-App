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

    public BackgroundSyncManager(
            UserService userService,
            IncidentService incidentService,
            AddressService addressService,
            NeighbourhoodService neighbourhoodService,
            CategoryService categoryService,
            ContractTemplateService contractTemplateService,
            MediaService mediaService
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
                        incidentSyncManager.syncWithBackend(conflict -> conflict.serverIncident());
                        break;
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    DailyLogger.logError("Sync", "Une erreur critique a stoppé la synchronisation en arrière-plan : " + e.getMessage(), e);
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