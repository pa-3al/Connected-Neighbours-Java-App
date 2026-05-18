package com.app.infrastructure.sync;

import com.app.domain.model.EventParticipation;

public record EventParticipationConflict(EventParticipation local, EventParticipation server) {}