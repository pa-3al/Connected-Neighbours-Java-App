package com.app.infrastructure.sync;

import com.app.domain.model.Event;

public record EventConflict(Event local, Event server) {}