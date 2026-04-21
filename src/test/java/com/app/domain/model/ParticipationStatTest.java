package com.app.domain.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class ParticipationStatTest {

    @Test
    void constructorShouldExposeGivenValues() {
        LocalDateTime lastActivity = LocalDateTime.now().minusHours(2);

        ParticipationStat stat = new ParticipationStat(
                "neighbor-1",
                "Ada",
                3,
                2,
                1,
                lastActivity
        );

        assertEquals("neighbor-1", stat.neighborId());
        assertEquals("Ada", stat.neighborName());
        assertEquals(3, stat.incidentsReported());
        assertEquals(2, stat.alertsCreated());
        assertEquals(1, stat.eventsAttended());
        assertEquals(lastActivity, stat.lastActivity());
    }

    @Test
    void constructorShouldAllowNullLastActivity() {
        ParticipationStat stat = new ParticipationStat(
                "neighbor-2",
                "Bob",
                0,
                0,
                0,
                null
        );

        assertNull(stat.lastActivity());
    }
}