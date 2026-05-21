package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.ServiceExpectedDate;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.ServiceExpectedDateService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;

public class ServiceExpectedDateSyncManager {

    private final ServiceExpectedDateService serviceExpectedDateService;
    private final ServiceExpectedDateBackendGateway backendGateway;
    private final ServiceExpectedDateSqliteGateway sqliteGateway;

    public ServiceExpectedDateSyncManager(ServiceExpectedDateService serviceExpectedDateService) {
        this(
                serviceExpectedDateService,
                new DatabaseConfig(),
                new ConfigProvider(),
                new AuthenticatedHttpClient()
        );
    }

    public ServiceExpectedDateSyncManager(
            ServiceExpectedDateService serviceExpectedDateService,
            DatabaseConfig databaseConfig,
            ConfigProvider configProvider,
            AuthenticatedHttpClient authenticatedHttpClient
    ) {
        this.serviceExpectedDateService = Objects.requireNonNull(serviceExpectedDateService);
        this.backendGateway = new ServiceExpectedDateBackendGateway(configProvider, authenticatedHttpClient);
        this.sqliteGateway = new ServiceExpectedDateSqliteGateway(databaseConfig);
    }

    public IncidentSyncReport syncWithBackend(Function<ServiceExpectedDateConflict, ServiceExpectedDate> conflictResolver) {
        Map<String, ServiceExpectedDate> localById = toMapById(serviceExpectedDateService.getAllServiceExpectedDates());
        Map<String, ServiceExpectedDate> serverById = backendGateway.fetchServiceExpectedDatesById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            ServiceExpectedDate local = localById.get(id);
            ServiceExpectedDate server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPairWithBackend(local, server, conflictResolver);
            metrics.record(outcome);
        }

        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPairWithBackend(ServiceExpectedDate local, ServiceExpectedDate server, Function<ServiceExpectedDateConflict, ServiceExpectedDate> conflictResolver) {
        if (local == null && server != null) {
            serviceExpectedDateService.updateServiceExpectedDate(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        if (local != null && server == null) {
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        if (local == null) {
            return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        }

        if (areEquivalent(local, server)) {
            serviceExpectedDateService.updateServiceExpectedDate(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        ServiceExpectedDate autoMerged = attemptAutoMerge(local, server);
        if (autoMerged != null) {
            serviceExpectedDateService.updateServiceExpectedDate(withSyncMetadata(autoMerged, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        ServiceExpectedDate resolved = conflictResolver.apply(new ServiceExpectedDateConflict(local, server));
        if (resolved == null) {
            serviceExpectedDateService.updateServiceExpectedDate(local.withSyncStatus(SyncStatus.CONFLICT));
            return IncidentSyncMetrics.SyncOutcome.CONFLICT_UNRESOLVED;
        }

        serviceExpectedDateService.updateServiceExpectedDate(withSyncMetadata(resolved, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.CONFLICT_RESOLVED;
    }

    private ServiceExpectedDate attemptAutoMerge(ServiceExpectedDate local, ServiceExpectedDate server) {
        return new ServiceExpectedDate(
                local.id(),
                mergeGeneric(local.startDate(), server.startDate()),
                mergeGeneric(local.endDate(), server.endDate()),
                mergeString(local.serviceId(), server.serviceId()),
                maxDate(local.lastModified(), server.lastModified()),
                SyncStatus.SYNCED
        );
    }

    private boolean areEquivalent(ServiceExpectedDate first, ServiceExpectedDate second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id())
                && Objects.equals(first.startDate(), second.startDate())
                && Objects.equals(first.endDate(), second.endDate())
                && stringsEquivalent(first.serviceId(), second.serviceId());
    }

    private Set<String> collectAllIds(Map<String, ServiceExpectedDate> localById, Map<String, ServiceExpectedDate> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, ServiceExpectedDate> toMapById(java.util.List<ServiceExpectedDate> dates) {
        Map<String, ServiceExpectedDate> byId = new HashMap<>();
        for (ServiceExpectedDate d : dates) {
            if (d != null && d.id() != null && !d.id().isBlank()) {
                byId.put(d.id(), d);
            }
        }
        return byId;
    }

    private boolean stringsEquivalent(String first, String second) {
        String valA = first == null ? "" : first.trim();
        String valB = second == null ? "" : second.trim();
        return valA.equals(valB);
    }

    private String mergeString(String localVal, String serverVal) {
        if (stringsEquivalent(localVal, serverVal)) {
            return localVal != null && !localVal.isBlank() ? localVal : (serverVal != null ? serverVal : "");
        }
        boolean localEmpty = localVal == null || localVal.isBlank();
        boolean serverEmpty = serverVal == null || serverVal.isBlank();
        if (localEmpty) return serverVal;
        if (serverEmpty) return localVal;
        return "___CONFLICT___";
    }

    private <T> T mergeGeneric(T localVal, T serverVal) {
        if (localVal == null && serverVal == null) return null;
        if (localVal == null) return serverVal;
        if (serverVal == null) return localVal;
        if (localVal.equals(serverVal)) return localVal;
        return null;
    }

    private ServiceExpectedDate withSyncMetadata(ServiceExpectedDate date, LocalDateTime syncTime) {
        return new ServiceExpectedDate(
                date.id(), date.startDate(), date.endDate(), date.serviceId(),
                syncTime, SyncStatus.SYNCED
        );
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}