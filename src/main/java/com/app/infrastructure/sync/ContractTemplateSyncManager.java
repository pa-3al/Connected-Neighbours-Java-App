package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.ContractTemplate;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.ContractTemplateService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.config.ConfigProvider;

public class ContractTemplateSyncManager {
    private final ContractTemplateService service;
    private final ContractTemplateBackendGateway backendGateway;

    public ContractTemplateSyncManager(ContractTemplateService service, ConfigProvider configProvider, AuthenticatedHttpClient authenticatedHttpClient) {
        this.service = Objects.requireNonNull(service);
        this.backendGateway = new ContractTemplateBackendGateway(configProvider, authenticatedHttpClient);
    }

    public IncidentSyncReport syncWithBackend(Function<ContractTemplateConflict, ContractTemplate> conflictResolver) {
        Map<String, ContractTemplate> localById = toMapById(service.getAllContractTemplates());
        Map<String, ContractTemplate> serverById = backendGateway.fetchContractTemplatesById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            ContractTemplate local = localById.get(id);
            ContractTemplate server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPair(local, server, conflictResolver);
            metrics.record(outcome);
        }
        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPair(ContractTemplate local, ContractTemplate server, Function<ContractTemplateConflict, ContractTemplate> conflictResolver) {
        if (local == null && server != null) {
            service.updateContractTemplate(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }
        if (local != null && server == null) return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        if (local == null) return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        if (areEquivalent(local, server)) {
            service.updateContractTemplate(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }
        service.updateContractTemplate(withSyncMetadata(server, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.PULLED;
    }

    private boolean areEquivalent(ContractTemplate first, ContractTemplate second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id()) && Objects.equals(first.updatedAt(), second.updatedAt());
    }

    private Set<String> collectAllIds(Map<String, ContractTemplate> localById, Map<String, ContractTemplate> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, ContractTemplate> toMapById(java.util.List<ContractTemplate> list) {
        Map<String, ContractTemplate> byId = new HashMap<>();
        for (ContractTemplate item : list) if (item != null && item.id() != null) byId.put(item.id(), item);
        return byId;
    }

    private ContractTemplate withSyncMetadata(ContractTemplate ct, LocalDateTime syncTime) {
        return new ContractTemplate(ct.id(), ct.contractType(), ct.languageId(), ct.documentPath(), ct.originalFileName(), ct.fileExtension(), ct.active(), ct.createdAt(), ct.updatedAt(), syncTime, SyncStatus.SYNCED);
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}