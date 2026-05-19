package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.EventParticipation;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.EventParticipationService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;

public class EventParticipationSyncManager {

    private final EventParticipationService service;
    private final EventParticipationBackendGateway backendGateway;

    public EventParticipationSyncManager(EventParticipationService service, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.service = Objects.requireNonNull(service);
        this.backendGateway = new EventParticipationBackendGateway(configProvider, authenticatedHttpClient);
    }

    public IncidentSyncReport syncWithBackend(Function<EventParticipationConflict, EventParticipation> conflictResolver) {
        Map<String, EventParticipation> localById = toMapById(service.getAllEventParticipations());
        Map<String, EventParticipation> serverById = backendGateway.fetchEventParticipationsById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            EventParticipation local = localById.get(id);
            EventParticipation server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPair(local, server, conflictResolver);
            metrics.record(outcome);
        }
        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPair(EventParticipation local, EventParticipation server, Function<EventParticipationConflict, EventParticipation> conflictResolver) {
        if (local == null && server != null) {
            service.updateEventParticipation(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }
        if (local != null && server == null) return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        if (local == null) return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        if (areEquivalent(local, server)) {
            service.updateEventParticipation(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }
        service.updateEventParticipation(withSyncMetadata(server, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.PULLED;
    }

    private boolean areEquivalent(EventParticipation first, EventParticipation second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id()) && Objects.equals(first.status(), second.status());
    }

    private Set<String> collectAllIds(Map<String, EventParticipation> localById, Map<String, EventParticipation> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, EventParticipation> toMapById(java.util.List<EventParticipation> list) {
        Map<String, EventParticipation> byId = new HashMap<>();
        for (EventParticipation item : list) if (item != null && item.id() != null) byId.put(item.id(), item);
        return byId;
    }

    private EventParticipation withSyncMetadata(EventParticipation ep, LocalDateTime syncTime) {
        return new EventParticipation(ep.id(), ep.subscribedAt(), ep.status(), ep.signatureUrl(), ep.rejectedReason(), ep.userId(), ep.eventId(), syncTime, SyncStatus.SYNCED);
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}