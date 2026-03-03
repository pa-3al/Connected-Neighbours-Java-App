package com.app.domain.model;

import java.time.LocalDateTime;

public record ParticipationStat(
    String neighborId,
    String neighborName,
    int incidentsReported,
    int alertsCreated,
    int eventsAttended,
    LocalDateTime lastActivity
) {
}
