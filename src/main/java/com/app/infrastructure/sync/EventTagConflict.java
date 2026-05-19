package com.app.infrastructure.sync;

import com.app.domain.model.EventTag;

public record EventTagConflict(EventTag local, EventTag server) {}