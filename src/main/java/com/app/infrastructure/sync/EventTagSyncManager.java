package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.EventTag;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.EventTagService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;

public class EventTagSyncManager {
    private final EventTagService service;
    private final EventTagBackendGateway backendGateway;

    public EventTagSyncManager(EventTagService service, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.service = Objects.requireNonNull(service);
        this.backendGateway = new EventTagBackendGateway(configProvider, authenticatedHttpClient);
    }

    public IncidentSyncReport syncWithBackend(Function<EventTagConflict, EventTag> conflictResolver) {
        Map<String, EventTag> localById = toMapById(service.getAllEventTags());
        Map<String, EventTag> serverById = backendGateway.fetchEventTagsById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            EventTag local = localById.get(id);
            EventTag server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPair(local, server, conflictResolver);
            metrics.record(outcome);
        }
        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPair(EventTag local, EventTag server, Function<EventTagConflict, EventTag> conflictResolver) {
        if (local == null && server != null) {
            service.updateEventTag(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }
        if (local != null && server == null) return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        if (local == null) return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        if (areEquivalent(local, server)) {
            service.updateEventTag(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }
        service.updateEventTag(withSyncMetadata(server, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.PULLED;
    }

    private boolean areEquivalent(EventTag first, EventTag second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.name(), second.name());
    }

    private Set<String> collectAllIds(Map<String, EventTag> localById, Map<String, EventTag> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, EventTag> toMapById(java.util.List<EventTag> list) {
        Map<String, EventTag> byId = new HashMap<>();
        for (EventTag item : list) if (item != null && item.name() != null) byId.put(item.name(), item);
        return byId;
    }

    private EventTag withSyncMetadata(EventTag et, LocalDateTime syncTime) {
        return new EventTag(et.name(), syncTime, SyncStatus.SYNCED);
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}