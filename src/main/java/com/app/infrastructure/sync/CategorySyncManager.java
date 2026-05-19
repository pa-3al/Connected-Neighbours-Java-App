package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Category;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.CategoryService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;

public class CategorySyncManager {
    private final CategoryService service;
    private final CategoryBackendGateway backendGateway;

    public CategorySyncManager(CategoryService service, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.service = Objects.requireNonNull(service);
        this.backendGateway = new CategoryBackendGateway(configProvider, authenticatedHttpClient);
    }

    public IncidentSyncReport syncWithBackend(Function<CategoryConflict, Category> conflictResolver) {
        Map<String, Category> localById = toMapById(service.getAllCategories());
        Map<String, Category> serverById = backendGateway.fetchCategoriesById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            Category local = localById.get(id);
            Category server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPair(local, server, conflictResolver);
            metrics.record(outcome);
        }
        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPair(Category local, Category server, Function<CategoryConflict, Category> conflictResolver) {
        if (local == null && server != null) {
            service.updateCategory(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }
        if (local != null && server == null) return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        if (local == null) return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        if (areEquivalent(local, server)) {
            service.updateCategory(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }
        service.updateCategory(withSyncMetadata(server, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.PULLED;
    }

    private boolean areEquivalent(Category first, Category second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id()) && Objects.equals(first.updatedAt(), second.updatedAt());
    }

    private Set<String> collectAllIds(Map<String, Category> localById, Map<String, Category> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, Category> toMapById(java.util.List<Category> list) {
        Map<String, Category> byId = new HashMap<>();
        for (Category item : list) if (item != null && item.id() != null) byId.put(item.id(), item);
        return byId;
    }

    private Category withSyncMetadata(Category c, LocalDateTime syncTime) {
        return new Category(c.id(), c.name(), c.type(), c.active(), c.createdAt(), c.updatedAt(), c.eventId(), syncTime, SyncStatus.SYNCED);
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}