package com.app.infrastructure.sync;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.app.domain.model.Address;
import com.app.domain.model.SyncStatus;
import com.app.domain.service.AddressService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;

public class AddressSyncManager {

    private static final String MERGE_CONFLICT_SENTINEL = "___CONFLICT___";

    private final AddressService addressService;
    private final AddressBackendGateway backendGateway;
    private final AddressSqliteGateway sqliteGateway;

    public AddressSyncManager(AddressService addressService) {
        this(
                addressService,
                new DatabaseConfig(),
                new ConfigProvider(),
                new AuthenticatedHttpClient()
        );
    }

    public AddressSyncManager(
            AddressService addressService,
            DatabaseConfig databaseConfig,
            ConfigProvider configProvider,
            AuthenticatedHttpClient authenticatedHttpClient
    ) {
        this.addressService = Objects.requireNonNull(addressService);
        this.backendGateway = new AddressBackendGateway(configProvider, authenticatedHttpClient);
        this.sqliteGateway = new AddressSqliteGateway(databaseConfig);
    }

    public IncidentSyncReport syncWithBackend(Function<AddressConflict, Address> conflictResolver) {
        Map<String, Address> localById = toMapById(addressService.getAllAddresses());
        Map<String, Address> serverById = backendGateway.fetchAddressesById();
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        for (String id : collectAllIds(localById, serverById)) {
            Address local = localById.get(id);
            Address server = serverById.get(id);
            IncidentSyncMetrics.SyncOutcome outcome = syncPairWithBackend(local, server, conflictResolver);
            metrics.record(outcome);
        }

        return metrics.toReport();
    }

    private IncidentSyncMetrics.SyncOutcome syncPairWithBackend(Address local, Address server, Function<AddressConflict, Address> conflictResolver) {
        if (local == null && server != null) {
            addressService.updateAddress(withSyncMetadata(server, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        if (local != null && server == null) {
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        if (local == null) {
            return IncidentSyncMetrics.SyncOutcome.SKIPPED;
        }

        if (areEquivalent(local, server)) {
            addressService.updateAddress(withSyncMetadata(local, maxDate(local.lastModified(), server.lastModified())));
            return IncidentSyncMetrics.SyncOutcome.UNCHANGED;
        }

        Address autoMerged = attemptAutoMerge(local, server);
        if (autoMerged != null) {
            addressService.updateAddress(withSyncMetadata(autoMerged, LocalDateTime.now()));
            return IncidentSyncMetrics.SyncOutcome.PULLED;
        }

        Address resolved = conflictResolver.apply(new AddressConflict(local, server));
        if (resolved == null) {
            addressService.updateAddress(local.withSyncStatus(SyncStatus.CONFLICT));
            return IncidentSyncMetrics.SyncOutcome.CONFLICT_UNRESOLVED;
        }

        addressService.updateAddress(withSyncMetadata(resolved, LocalDateTime.now()));
        return IncidentSyncMetrics.SyncOutcome.CONFLICT_RESOLVED;
    }

    private Address attemptAutoMerge(Address local, Address server) {
        String streetNumber = mergeString(local.streetNumber(), server.streetNumber());
        if (isMergeConflict(streetNumber)) return null;

        String streetName = mergeString(local.streetName(), server.streetName());
        if (isMergeConflict(streetName)) return null;

        String city = mergeString(local.city(), server.city());
        if (isMergeConflict(city)) return null;

        return new Address(
                local.id(),
                streetNumber,
                mergeString(local.addressLine2(), server.addressLine2()),
                streetName,
                city,
                mergeString(local.postalCode(), server.postalCode()),
                mergeString(local.region(), server.region()),
                mergeString(local.countryCode(), server.countryCode()),
                mergeString(local.location(), server.location()),
                mergeGeneric(local.active(), server.active()),
                mergeString(local.neighbourhoodId(), server.neighbourhoodId()),
                mergeString(local.userId(), server.userId()),
                maxDate(local.lastModified(), server.lastModified()),
                SyncStatus.SYNCED
        );
    }

    private boolean areEquivalent(Address first, Address second) {
        if (first == null || second == null) return false;
        return Objects.equals(first.id(), second.id())
                && stringsEquivalent(first.streetName(), second.streetName())
                && stringsEquivalent(first.streetNumber(), second.streetNumber())
                && stringsEquivalent(first.city(), second.city());
    }

    private Set<String> collectAllIds(Map<String, Address> localById, Map<String, Address> remoteById) {
        Set<String> allIds = new TreeSet<>(localById.keySet());
        allIds.addAll(remoteById.keySet());
        return allIds;
    }

    private Map<String, Address> toMapById(java.util.List<Address> addresses) {
        Map<String, Address> byId = new HashMap<>();
        for (Address a : addresses) {
            if (a != null && a.id() != null && !a.id().isBlank()) {
                byId.put(a.id(), a);
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

    private Address withSyncMetadata(Address address, LocalDateTime syncTime) {
        return new Address(
                address.id(), address.streetNumber(), address.addressLine2(), address.streetName(),
                address.city(), address.postalCode(), address.region(), address.countryCode(),
                address.location(), address.active(), address.neighbourhoodId(), address.userId(),
                syncTime, SyncStatus.SYNCED
        );
    }

    private LocalDateTime maxDate(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second == null ? LocalDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}