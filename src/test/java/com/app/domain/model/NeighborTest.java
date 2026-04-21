package com.app.domain.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class NeighborTest {

    @Test
    void createShouldBuildActiveNeighborWithCurrentJoinDate() {
        LocalDateTime before = LocalDateTime.now();

        Neighbor neighbor = Neighbor.create("n-1", "Ada", "ada@test.com", "10 rue de Paris");

        LocalDateTime after = LocalDateTime.now();

        assertEquals("n-1", neighbor.id());
        assertEquals("Ada", neighbor.name());
        assertEquals("ada@test.com", neighbor.email());
        assertEquals("10 rue de Paris", neighbor.address());
        assertTrue(neighbor.isActive());
        assertNotNull(neighbor.joinedAt());
        assertFalse(neighbor.joinedAt().isBefore(before));
        assertFalse(neighbor.joinedAt().isAfter(after));
    }
}