package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.EventPlanning;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.EventPlanningService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;

public class EventPlanningSyncManager {

    private final EventPlanningService service;
    private final EventPlanningBackendGateway backendGateway;

    public EventPlanningSyncManager(EventPlanningService service, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.service = Objects.requireNonNull(service);
        this.backendGateway = new EventPlanningBackendGateway(configProvider, authenticatedHttpClient);
    }

    public IncidentSyncReport syncWithBackend(Function<EventPlanningConflict, EventPlanning> conflictResolver) {
        Map<String, EventPlanning> localById = toMapById(service.getAllEventPlannings());
        Map<String, EventPlanning> serverById = backendGateway.fetchEventPlanningsById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            EventPlanning local = localById.get(id);
            EventPlanning server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPair(local, server, conflictResolver);
            metrics.record(outcome);
        }
        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPair(EventPlanning local, EventPlanning server, Function<EventPlanningConflict, EventPlanning> conflictResolver) {
        if (local == null && server != null) {
            service.updateEventPlanning(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }
        if (local != null && server == null) return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        if (local == null) return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        if (areEquivalent(local, server)) {
            service.updateEventPlanning(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }
        service.updateEventPlanning(withSyncMetadata(server, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.PULLED;
    }

    private boolean areEquivalent(EventPlanning first, EventPlanning second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id()) && Objects.equals(first.startDate(), second.startDate()) && Objects.equals(first.endDate(), second.endDate());
    }

    private Set<String> collectAllIds(Map<String, EventPlanning> localById, Map<String, EventPlanning> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, EventPlanning> toMapById(java.util.List<EventPlanning> list) {
        Map<String, EventPlanning> byId = new HashMap<>();
        for (EventPlanning item : list) if (item != null && item.id() != null) byId.put(item.id(), item);
        return byId;
    }

    private EventPlanning withSyncMetadata(EventPlanning ep, LocalDateTime syncTime) {
        return new EventPlanning(ep.id(), ep.startDate(), ep.endDate(), ep.eventId(), syncTime, SyncStatus.SYNCED);
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}