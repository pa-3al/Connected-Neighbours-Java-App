package com.app.domain.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SyncStatusTest {

    @Test
    void valuesShouldMatchExpectedOrder() {
        SyncStatus[] expected = {
                SyncStatus.SYNCED,
                SyncStatus.PENDING,
                SyncStatus.CONFLICT,
                SyncStatus.LOCAL_ONLY
        };

        assertArrayEquals(expected, SyncStatus.values());
    }

    @Test
    void valueOfShouldResolveEnumByName() {
        assertEquals(SyncStatus.SYNCED, SyncStatus.valueOf("SYNCED"));
        assertEquals(SyncStatus.PENDING, SyncStatus.valueOf("PENDING"));
        assertEquals(SyncStatus.CONFLICT, SyncStatus.valueOf("CONFLICT"));
        assertEquals(SyncStatus.LOCAL_ONLY, SyncStatus.valueOf("LOCAL_ONLY"));
    }
}