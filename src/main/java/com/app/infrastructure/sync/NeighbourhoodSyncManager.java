package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Neighbourhood;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.NeighbourhoodService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;

public class NeighbourhoodSyncManager {

    private static final String MERGE_CONFLICT_SENTINEL = "___CONFLICT___";

    private final NeighbourhoodService neighbourhoodService;
    private final NeighbourhoodBackendGateway backendGateway;
    private final NeighbourhoodSqliteGateway sqliteGateway;

    public NeighbourhoodSyncManager(NeighbourhoodService neighbourhoodService) {
        this(
                neighbourhoodService,
                new DatabaseConfig(),
                new ConfigProvider(),
                new AuthenticatedHttpClient()
        );
    }

    public NeighbourhoodSyncManager(
            NeighbourhoodService neighbourhoodService,
            DatabaseConfig databaseConfig,
            ConfigProvider configProvider,
            AuthenticatedHttpClient authenticatedHttpClient
    ) {
        this.neighbourhoodService = Objects.requireNonNull(neighbourhoodService);
        this.backendGateway = new NeighbourhoodBackendGateway(configProvider, authenticatedHttpClient);
        this.sqliteGateway = new NeighbourhoodSqliteGateway(databaseConfig);
    }

    public IncidentSyncReport syncWithBackend(Function<NeighbourhoodConflict, Neighbourhood> conflictResolver) {
        Map<String, Neighbourhood> localById = toMapById(neighbourhoodService.getAllNeighbourhoods());
        Map<String, Neighbourhood> serverById = backendGateway.fetchNeighbourhoodsById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            Neighbourhood local = localById.get(id);
            Neighbourhood server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPairWithBackend(local, server, conflictResolver);
            metrics.record(outcome);
        }

        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPairWithBackend(Neighbourhood local, Neighbourhood server, Function<NeighbourhoodConflict, Neighbourhood> conflictResolver) {
        if (local == null && server != null) {
            neighbourhoodService.updateNeighbourhood(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        if (local != null && server == null) {
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        if (local == null) {
            return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        }

        if (areEquivalent(local, server)) {
            neighbourhoodService.updateNeighbourhood(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        Neighbourhood autoMerged = attemptAutoMerge(local, server);
        if (autoMerged != null) {
            neighbourhoodService.updateNeighbourhood(withSyncMetadata(autoMerged, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        Neighbourhood resolved = conflictResolver.apply(new NeighbourhoodConflict(local, server));
        if (resolved == null) {
            neighbourhoodService.updateNeighbourhood(local.withSyncStatus(SyncStatus.CONFLICT));
            return IncidentSyncMetrics.SyncOutcome.CONFLICT_UNRESOLVED;
        }

        neighbourhoodService.updateNeighbourhood(withSyncMetadata(resolved, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.CONFLICT_RESOLVED;
    }

    private Neighbourhood attemptAutoMerge(Neighbourhood local, Neighbourhood server) {
        String name = mergeString(local.name(), server.name());
        if (isMergeConflict(name)) return null;

        String city = mergeString(local.city(), server.city());
        if (isMergeConflict(city)) return null;

        return new Neighbourhood(
                local.id(),
                name,
                mergeString(local.description(), server.description()),
                city,
                mergeString(local.postalCode(), server.postalCode()),
                mergeString(local.countryCode(), server.countryCode()),
                mergeGeneric(local.estimatedPopulation(), server.estimatedPopulation()),
                mergeString(local.polygon(), server.polygon()),
                mergeGeneric(local.area(), server.area()),
                maxDate(local.lastModified(), server.lastModified()),
                SyncStatus.SYNCED
        );
    }

    private boolean areEquivalent(Neighbourhood first, Neighbourhood second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id())
                && stringsEquivalent(first.name(), second.name())
                && stringsEquivalent(first.city(), second.city());
    }

    private Set<String> collectAllIds(Map<String, Neighbourhood> localById, Map<String, Neighbourhood> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, Neighbourhood> toMapById(java.util.List<Neighbourhood> neighbourhoods) {
        Map<String, Neighbourhood> byId = new HashMap<>();
        for (Neighbourhood n : neighbourhoods) {
            if (n != null && n.id() != null && !n.id().isBlank()) {
                byId.put(n.id(), n);
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

    private Neighbourhood withSyncMetadata(Neighbourhood neighbourhood, LocalDateTime syncTime) {
        return new Neighbourhood(
                neighbourhood.id(), neighbourhood.name(), neighbourhood.description(),
                neighbourhood.city(), neighbourhood.postalCode(), neighbourhood.countryCode(),
                neighbourhood.estimatedPopulation(), neighbourhood.polygon(), neighbourhood.area(),
                syncTime, SyncStatus.SYNCED
        );
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}