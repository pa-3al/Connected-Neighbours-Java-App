package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Event;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.EventService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;

public class EventSyncManager {
    private final EventService service;
    private final EventBackendGateway backendGateway;

    public EventSyncManager(EventService service, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.service = Objects.requireNonNull(service);
        this.backendGateway = new EventBackendGateway(configProvider, authenticatedHttpClient);
    }

    public IncidentSyncReport syncWithBackend(Function<EventConflict, Event> conflictResolver) {
        Map<String, Event> localById = toMapById(service.getAllEvents());
        Map<String, Event> serverById = backendGateway.fetchEventsById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            Event local = localById.get(id);
            Event server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPair(local, server, conflictResolver);
            metrics.record(outcome);
        }
        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPair(Event local, Event server, Function<EventConflict, Event> conflictResolver) {
        if (local == null && server != null) {
            service.updateEvent(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }
        if (local != null && server == null) return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        if (local == null) return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        if (areEquivalent(local, server)) {
            service.updateEvent(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }
        service.updateEvent(withSyncMetadata(server, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.PULLED;
    }

    private boolean areEquivalent(Event first, Event second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id()) && Objects.equals(first.name(), second.name());
    }

    private Set<String> collectAllIds(Map<String, Event> localById, Map<String, Event> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, Event> toMapById(java.util.List<Event> list) {
        Map<String, Event> byId = new HashMap<>();
        for (Event item : list) if (item != null && item.id() != null) byId.put(item.id(), item);
        return byId;
    }

    private Event withSyncMetadata(Event e, LocalDateTime syncTime) {
        return new Event(e.id(), e.name(), e.description(), e.points(), e.realMoneyPrice(), e.requireValidation(), e.signatureUrl(), e.contractId(), e.addressId(), e.createdByUserId(), e.approvedByModeratorId(), e.approvedByAdminId(), syncTime, SyncStatus.SYNCED);
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}