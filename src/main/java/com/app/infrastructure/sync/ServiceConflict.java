package com.app.infrastructure.sync;

import com.app.domain.model.Service;

public record ServiceConflict(Service local, Service server) {}