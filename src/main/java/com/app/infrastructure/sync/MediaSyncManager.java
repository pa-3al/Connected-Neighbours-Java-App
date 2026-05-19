package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Media;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.MediaService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;

public class MediaSyncManager {
    private final MediaService service;
    private final MediaBackendGateway backendGateway;

    public MediaSyncManager(MediaService service, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.service = Objects.requireNonNull(service);
        this.backendGateway = new MediaBackendGateway(configProvider, authenticatedHttpClient);
    }

    public IncidentSyncReport syncWithBackend(Function<MediaConflict, Media> conflictResolver) {
        Map<String, Media> localById = toMapById(service.getAllMedia());
        Map<String, Media> serverById = backendGateway.fetchMediaById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            Media local = localById.get(id);
            Media server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPair(local, server, conflictResolver);
            metrics.record(outcome);
        }
        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPair(Media local, Media server, Function<MediaConflict, Media> conflictResolver) {
        if (local == null && server != null) {
            service.updateMedia(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }
        if (local != null && server == null) return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        if (local == null) return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        if (areEquivalent(local, server)) {
            service.updateMedia(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }
        service.updateMedia(withSyncMetadata(server, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.PULLED;
    }

    private boolean areEquivalent(Media first, Media second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id()) && Objects.equals(first.url(), second.url());
    }

    private Set<String> collectAllIds(Map<String, Media> localById, Map<String, Media> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, Media> toMapById(java.util.List<Media> list) {
        Map<String, Media> byId = new HashMap<>();
        for (Media item : list) if (item != null && item.id() != null) byId.put(item.id(), item);
        return byId;
    }

    private Media withSyncMetadata(Media m, LocalDateTime syncTime) {
        return new Media(m.id(), m.type(), m.url(), m.fileExtension(), m.neighbourhoodId(), m.eventId(), syncTime, SyncStatus.SYNCED);
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}