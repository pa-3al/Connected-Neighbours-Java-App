package com.app.infrastructure.sync;

public record IncidentSyncReport(
    int pushedToServer,
    int pulledFromServer,
    int conflictsResolved,
    int conflictsUnresolved,
    int unchanged
) {
}
