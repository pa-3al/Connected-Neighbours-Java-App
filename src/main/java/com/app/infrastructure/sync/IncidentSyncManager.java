package com.app.infrastructure.sync;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.IncidentService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;

public class IncidentSyncManager {

    private static final String MERGE_CONFLICT_SENTINEL = "___CONFLICT___";

    private final IncidentService incidentService;
    private final IncidentBackendGateway backendGateway;
    private final IncidentSqliteGateway sqliteGateway;

    public IncidentSyncManager(IncidentService incidentService) {
        this(
                incidentService,
                new DatabaseConfig(),
                new ConfigProvider(),
                new AuthenticatedHttpClient()
        );
    }

    public IncidentSyncManager(IncidentService incidentService, DatabaseConfig databaseConfig) {
        this(
                incidentService,
                databaseConfig,
                new ConfigProvider(),
                new AuthenticatedHttpClient()
        );
    }

    IncidentSyncManager(
            IncidentService incidentService,
            DatabaseConfig databaseConfig,
            ConfigProvider configProvider,
            AuthenticatedHttpClient authenticatedHttpClient
    ) {
        this.incidentService = Objects.requireNonNull(incidentService, "incidentService must not be null");
        this.backendGateway = new IncidentBackendGateway(
                Objects.requireNonNull(configProvider, "configProvider must not be null"),
                Objects.requireNonNull(authenticatedHttpClient, "authenticatedHttpClient must not be null")
        );
        this.sqliteGateway = new IncidentSqliteGateway(
                Objects.requireNonNull(databaseConfig, "databaseConfig must not be null")
        );
    }

    public IncidentSyncReport syncWithBackend(Function<IncidentConflict, Incident> conflictResolver) {
        Objects.requireNonNull(conflictResolver, "conflictResolver must not be null");

        Map<String, Incident> localById = toMapById(incidentService.getAllIncidents());
        Map<String, Incident> serverById = backendGateway.fetchIncidentsById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            Incident local = localById.get(id);
            Incident server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncIncidentPairWithBackend(local, server, conflictResolver);
            metrics.record(outcome);
        }

        return metrics.toReport();
    }

    public IncidentSyncReport sync(Path serverDatabasePath, Function<IncidentConflict, Incident> conflictResolver) {
        Objects.requireNonNull(serverDatabasePath, "serverDatabasePath must not be null");
        Objects.requireNonNull(conflictResolver, "conflictResolver must not be null");

        Map<String, Incident> localById = toMapById(incidentService.getAllIncidents());

        try (Connection localConnection = sqliteGateway.openLocalConnection();
             Connection serverConnection = sqliteGateway.openServerConnection(serverDatabasePath)) {

            sqliteGateway.ensureSyncSchema(localConnection, serverConnection);
            Map<String, Incident> serverById = sqliteGateway.loadIncidentsById(serverConnection);
            IncidentSyncMetrics metrics = new IncidentSyncMetrics();

            for (String id : collectAllIds(localById, serverById)) {
                Incident local = localById.get(id);
                Incident server = serverById.get(id);
                IncidentSyncMetrics.SyncOutcome outcome = syncIncidentPairWithDatabase(serverConnection, local, server, conflictResolver);
                metrics.record(outcome);
            }

            return metrics.toReport();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to synchronize reports", e);
        }
    }

        private IncidentSyncMetrics.SyncOutcome syncIncidentPairWithBackend(
            Incident local,
            Incident server,
            Function<IncidentConflict, Incident> conflictResolver
    ) {
        if (local == null && server != null) {
            incidentService.updateIncident(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        if (local != null && server == null) {
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        if (local == null || server == null) {
            return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        }

        if (areEquivalent(local, server)) {
            Incident synced = withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified()));
            incidentService.updateIncident(synced);
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        Incident autoMerged = attemptAutoMerge(local, server);
        if (autoMerged != null) {
            Incident synced = withSyncMetadata(autoMerged, LocalDateTime.now());
            incidentService.updateIncident(synced);

            if (shouldPushAdminResponseAfterAutoMerge(synced, server)) {
                backendGateway.pushAdminResponse(synced.id(), synced.adminResponseMessage());
                return IncidentSyncMetrics.SyncOutcome.PUSHED;
            }
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        Incident resolved = conflictResolver.apply(new IncidentConflict(local, server));
        if (resolved == null) {
            incidentService.updateIncident(local.withSyncStatus(SyncStatus.CONFLICT));
            return IncidentSyncMetrics.SyncOutcome.CONFLICT_UNRESOLVED;
        }

        Incident synced = withSyncMetadata(resolved, LocalDateTime.now());
        incidentService.updateIncident(synced);
        if (hasAdminResponse(synced)) {
            backendGateway.pushAdminResponse(synced.id(), synced.adminResponseMessage());
        }
        return IncidentSyncMetrics.SyncOutcome.CONFLICT_RESOLVED;
    }

    private IncidentSyncMetrics.SyncOutcome syncIncidentPairWithDatabase(
            Connection serverConnection,
            Incident local,
            Incident server,
            Function<IncidentConflict, Incident> conflictResolver
    ) throws SQLException {
        if (local == null && server != null) {
            incidentService.updateIncident(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        if (local != null && server == null) {
            Incident synced = withSyncMetadata(local, LocalDateTime.now());
            incidentService.updateIncident(synced);
            sqliteGateway.upsertIncident(serverConnection, synced);
            return IncidentSyncMetrics.SyncOutcome.PUSHED;
        }

        if (local == null || server == null) {
            return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        }

        if (areEquivalent(local, server)) {
            Incident synced = withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified()));
            incidentService.updateIncident(synced);
            sqliteGateway.upsertIncident(serverConnection, synced);
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        Incident autoMerged = attemptAutoMerge(local, server);
        if (autoMerged != null) {
            Incident synced = withSyncMetadata(autoMerged, LocalDateTime.now());
            incidentService.updateIncident(synced);
            sqliteGateway.upsertIncident(serverConnection, synced);
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        Incident resolved = conflictResolver.apply(new IncidentConflict(local, server));
        if (resolved == null) {
            incidentService.updateIncident(local.withSyncStatus(SyncStatus.CONFLICT));
            return IncidentSyncMetrics.SyncOutcome.CONFLICT_UNRESOLVED;
        }

        Incident synced = withSyncMetadata(resolved, LocalDateTime.now());
        incidentService.updateIncident(synced);
        sqliteGateway.upsertIncident(serverConnection, synced);
        return IncidentSyncMetrics.SyncOutcome.CONFLICT_RESOLVED;
    }

    private Set<String> collectAllIds(Map<String, Incident> localById, Map<String, Incident> remoteById) {
        Set<String> allIds = new TreeSet<>();
        allIds.addAll(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private boolean hasAdminResponse(Incident incident) {
        return incident != null
                && incident.adminResponseMessage() != null
                && !incident.adminResponseMessage().isBlank();
    }

    private boolean shouldPushAdminResponseAfterAutoMerge(Incident merged, Incident originalServer) {
        return hasAdminResponse(merged)
                && (originalServer == null
                || originalServer.adminResponseMessage() == null
                || originalServer.adminResponseMessage().isBlank());
    }

    private Incident attemptAutoMerge(Incident local, Incident server) {
        String title = mergeString(local.title(), server.title());
        if (isMergeConflict(title)) {
            return null;
        }

        String description = mergeString(local.description(), server.description());
        if (isMergeConflict(description)) {
            return null;
        }

        IncidentCategory category = mergeGeneric(local.category(), server.category());
        if (category == null && (local.category() != null || server.category() != null)) {
            return null;
        }

        IncidentStatus status = mergeGeneric(local.status(), server.status());
        if (status == null && (local.status() != null || server.status() != null)) {
            return null;
        }

        String reportedByUserId = mergeString(local.reportedByUserId(), server.reportedByUserId());
        if (isMergeConflict(reportedByUserId)) {
            return null;
        }

        String reportedBy = server.reportedBy() != null && !server.reportedBy().isBlank()
                ? server.reportedBy()
                : local.reportedBy();

        String adminResponse = mergeString(local.adminResponseMessage(), server.adminResponseMessage());
        if (isMergeConflict(adminResponse)) {
            return null;
        }

        LocalDateTime reportedAt = server.reportedAt() != null ? server.reportedAt() : local.reportedAt();
        LocalDateTime resolvedAt = server.resolvedAt() != null ? server.resolvedAt() : local.resolvedAt();

        return new Incident(
                local.id(),
                title,
                description,
                category,
                status,
                reportedByUserId,
                reportedBy,
                adminResponse,
                reportedAt,
                resolvedAt,
                maxDate(local.lastModified(), server.lastModified()),
                SyncStatus.SYNCED
        );
    }

    private boolean areEquivalent(Incident first, Incident second) {
        if (first == null || second == null) {
            return false;
        }
        return Objects.equals(first.id(), second.id())
                && stringsEquivalent(first.title(), second.title())
                && stringsEquivalent(first.description(), second.description())
                && Objects.equals(first.category(), second.category())
                && Objects.equals(first.status(), second.status())
                && stringsEquivalent(first.adminResponseMessage(), second.adminResponseMessage())
                && stringsEquivalent(first.reportedByUserId(), second.reportedByUserId());
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
        if (localEmpty) {
            return serverVal;
        }
        if (serverEmpty) {
            return localVal;
        }
        return MERGE_CONFLICT_SENTINEL;
    }

    private boolean isMergeConflict(String value) {
        return MERGE_CONFLICT_SENTINEL.equals(value);
    }

    private <T> T mergeGeneric(T localVal, T serverVal) {
        if (localVal == null && serverVal == null) {
            return null;
        }
        if (localVal == null) {
            return serverVal;
        }
        if (serverVal == null) {
            return localVal;
        }
        if (localVal.equals(serverVal)) {
            return localVal;
        }
        return null;
    }

    private Map<String, Incident> toMapById(java.util.List<Incident> incidents) {
        Map<String, Incident> byId = new HashMap<>();
        for (Incident incident : incidents) {
            if (incident != null && incident.id() != null && !incident.id().isBlank()) {
                byId.put(incident.id(), incident);
            }
        }
        return byId;
    }

    private Incident withSyncMetadata(Incident incident, LocalDateTime syncTime) {
        return new Incident(
                incident.id(),
                incident.title(),
                incident.description(),
                incident.category(),
                incident.status(),
                incident.reportedByUserId(),
                incident.reportedBy(),
                incident.adminResponseMessage(),
                incident.reportedAt(),
                incident.resolvedAt(),
                syncTime,
                SyncStatus.SYNCED
        );
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second == null ? LocalDateTime.now() : second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

}
