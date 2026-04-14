package com.app.infrastructure.sync;

final class IncidentSyncMetrics {

    enum SyncOutcome {
        PUSHED,
        PULLED,
        CONFLICT_RESOLVED,
        CONFLICT_UNRESOLVED,
        UNCHANGED,
        SKIPPED
    }

    private int pushedToServer;
    private int pulledFromServer;
    private int conflictsResolved;
    private int conflictsUnresolved;
    private int unchanged;

    void record(SyncOutcome outcome) {
        switch (outcome) {
            case PUSHED -> pushedToServer++;
            case PULLED -> pulledFromServer++;
            case CONFLICT_RESOLVED -> conflictsResolved++;
            case CONFLICT_UNRESOLVED -> conflictsUnresolved++;
            case UNCHANGED -> unchanged++;
            case SKIPPED -> {
            }
        }
    }

    IncidentSyncReport toReport() {
        return new IncidentSyncReport(
                pushedToServer,
                pulledFromServer,
                conflictsResolved,
                conflictsUnresolved,
                unchanged
        );
    }
}
