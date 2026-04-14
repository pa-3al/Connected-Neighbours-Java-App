package com.app.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DownloadProgressTest {

    @Test
    void ofShouldComputePercentage() {
        DownloadProgress progress = DownloadProgress.of(25, 100);

        assertEquals(25L, progress.bytesDownloaded());
        assertEquals(100L, progress.totalBytes());
        assertEquals(25.0d, progress.percentage());
    }

    @Test
    void ofShouldReturnZeroPercentageWhenTotalIsZero() {
        DownloadProgress progress = DownloadProgress.of(25, 0);

        assertEquals(0.0d, progress.percentage());
        assertFalse(progress.isComplete());
    }

    @Test
    void isCompleteShouldReturnTrueWhenDownloadedReachesTotal() {
        DownloadProgress progress = DownloadProgress.of(100, 100);

        assertTrue(progress.isComplete());
    }
}