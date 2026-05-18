package com.app.infrastructure.sync;

import com.app.domain.service.AddressService;
import com.app.domain.service.IncidentService;
import com.app.domain.service.NeighbourhoodService;
import com.app.domain.service.UserService;
import com.app.infrastructure.util.ConnectivityUtil;

public class BackgroundSyncManager {

    private final UserService userService;
    private final IncidentSyncManager incidentSyncManager;
    private final AddressSyncManager addressSyncManager;
    private final NeighbourhoodSyncManager neighbourhoodSyncManager;

    public BackgroundSyncManager(
            UserService userService,
            IncidentService incidentService,
            AddressService addressService,
            NeighbourhoodService neighbourhoodService
    ) {
        this.userService = userService;
        this.incidentSyncManager = new IncidentSyncManager(incidentService);
        this.addressSyncManager = new AddressSyncManager(addressService);
        this.neighbourhoodSyncManager = new NeighbourhoodSyncManager(neighbourhoodService);
    }

    public void startAutomaticSyncOnStartup() {
        Thread syncThread = new Thread(() -> {
            while (true) {
                try {
                    if (ConnectivityUtil.checkConnectivity()) {
                        userService.syncUsers();
                        neighbourhoodSyncManager.syncWithBackend(conflict -> conflict.server());
                        addressSyncManager.syncWithBackend(conflict -> conflict.server());
                        incidentSyncManager.syncWithBackend(conflict -> conflict.serverIncident());
                        break;
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
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