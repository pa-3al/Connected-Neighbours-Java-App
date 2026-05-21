package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Service;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.ServiceService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;

public class ServiceSyncManager {

    private static final String MERGE_CONFLICT_SENTINEL = "___CONFLICT___";

    private final ServiceService serviceService;
    private final ServiceBackendGateway backendGateway;
    private final ServiceSqliteGateway sqliteGateway;

    public ServiceSyncManager(ServiceService serviceService) {
        this(
                serviceService,
                new DatabaseConfig(),
                new ConfigProvider(),
                new AuthenticatedHttpClient()
        );
    }

    public ServiceSyncManager(
            ServiceService serviceService,
            DatabaseConfig databaseConfig,
            ConfigProvider configProvider,
            AuthenticatedHttpClient authenticatedHttpClient
    ) {
        this.serviceService = Objects.requireNonNull(serviceService);
        this.backendGateway = new ServiceBackendGateway(configProvider, authenticatedHttpClient);
        this.sqliteGateway = new ServiceSqliteGateway(databaseConfig);
    }

    public IncidentSyncReport syncWithBackend(Function<ServiceConflict, Service> conflictResolver) {
        Map<String, Service> localById = toMapById(serviceService.getAllServices());
        Map<String, Service> serverById = backendGateway.fetchServicesById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            Service local = localById.get(id);
            Service server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPairWithBackend(local, server, conflictResolver);
            metrics.record(outcome);
        }

        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPairWithBackend(Service local, Service server, Function<ServiceConflict, Service> conflictResolver) {
        if (local == null && server != null) {
            serviceService.updateService(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        if (local != null && server == null) {
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        if (local == null) {
            return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        }

        if (areEquivalent(local, server)) {
            serviceService.updateService(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        Service autoMerged = attemptAutoMerge(local, server);
        if (autoMerged != null) {
            serviceService.updateService(withSyncMetadata(autoMerged, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        Service resolved = conflictResolver.apply(new ServiceConflict(local, server));
        if (resolved == null) {
            serviceService.updateService(local.withSyncStatus(SyncStatus.CONFLICT));
            return IncidentSyncMetrics.SyncOutcome.CONFLICT_UNRESOLVED;
        }

        serviceService.updateService(withSyncMetadata(resolved, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.CONFLICT_RESOLVED;
    }

    private Service attemptAutoMerge(Service local, Service server) {
        String type = mergeString(local.type(), server.type());
        if (isMergeConflict(type)) return null;

        String title = mergeString(local.title(), server.title());
        if (isMergeConflict(title)) return null;

        String description = mergeString(local.description(), server.description());
        if (isMergeConflict(description)) return null;

        String status = mergeString(local.status(), server.status());
        if (isMergeConflict(status)) return null;

        return new Service(
                local.id(),
                type,
                title,
                description,
                mergeGeneric(local.points(), server.points()),
                status,
                mergeString(local.moderatorComment(), server.moderatorComment()),
                mergeString(local.signatureUrl(), server.signatureUrl()),
                mergeString(local.contractId(), server.contractId()),
                mergeString(local.serviceTypeId(), server.serviceTypeId()),
                mergeString(local.addressId(), server.addressId()),
                mergeString(local.createdByUserId(), server.createdByUserId()),
                mergeString(local.approvedByModeratorId(), server.approvedByModeratorId()),
                mergeGeneric(local.createdAt(), server.createdAt()),
                mergeGeneric(local.updatedAt(), server.updatedAt()),
                maxDate(local.lastModified(), server.lastModified()),
                SyncStatus.SYNCED
        );
    }

    private boolean areEquivalent(Service first, Service second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id())
                && stringsEquivalent(first.type(), second.type())
                && stringsEquivalent(first.title(), second.title())
                && stringsEquivalent(first.description(), second.description())
                && Objects.equals(first.points(), second.points())
                && stringsEquivalent(first.status(), second.status());
    }

    private Set<String> collectAllIds(Map<String, Service> localById, Map<String, Service> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, Service> toMapById(java.util.List<Service> services) {
        Map<String, Service> byId = new HashMap<>();
        for (Service s : services) {
            if (s != null && s.id() != null && !s.id().isBlank()) {
                byId.put(s.id(), s);
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
        return MERGE_CONFLICT_SENTINEL;
    }

    private boolean isMergeConflict(String value) {
        return MERGE_CONFLICT_SENTINEL.equals(value);
    }

    private <T> T mergeGeneric(T localVal, T serverVal) {
        if (localVal == null && serverVal == null) return null;
        if (localVal == null) return serverVal;
        if (serverVal == null) return localVal;
        if (localVal.equals(serverVal)) return localVal;
        return null;
    }

    private Service withSyncMetadata(Service service, LocalDateTime syncTime) {
        return new Service(
                service.id(), service.type(), service.title(), service.description(),
                service.points(), service.status(), service.moderatorComment(),
                service.signatureUrl(), service.contractId(), service.serviceTypeId(),
                service.addressId(), service.createdByUserId(), service.approvedByModeratorId(),
                service.createdAt(), service.updatedAt(), syncTime, SyncStatus.SYNCED
        );
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}