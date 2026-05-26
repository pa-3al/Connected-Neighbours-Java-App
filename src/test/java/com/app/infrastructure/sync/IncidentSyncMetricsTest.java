package com.app.infrastructure.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IncidentSyncMetricsTest {

    @Test
    void toReportShouldReturnZeroCountsBeforeRecordingOutcomes() {
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        assertEquals(new IncidentSyncReport(0, 0, 0, 0, 0), metrics.toReport());
    }

    @Test
    void recordShouldCountEachSyncOutcomeAndIgnoreSkipped() {
        IncidentSyncMetrics metrics = new IncidentSyncMetrics();

        metrics.record(IncidentSyncMetrics.SyncOutcome.PUSHED);
        metrics.record(IncidentSyncMetrics.SyncOutcome.PULLED);
        metrics.record(IncidentSyncMetrics.SyncOutcome.PULLED);
        metrics.record(IncidentSyncMetrics.SyncOutcome.CONFLICT_RESOLVED);
        metrics.record(IncidentSyncMetrics.SyncOutcome.CONFLICT_UNRESOLVED);
        metrics.record(IncidentSyncMetrics.SyncOutcome.UNCHANGED);
        metrics.record(IncidentSyncMetrics.SyncOutcome.SKIPPED);

        assertEquals(new IncidentSyncReport(1, 2, 1, 1, 1), metrics.toReport());
    }
}
