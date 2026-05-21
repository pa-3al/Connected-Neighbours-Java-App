package com.app.infrastructure.sync;

import com.app.domain.model.ServiceExpectedDate;

public record ServiceExpectedDateConflict(ServiceExpectedDate local, ServiceExpectedDate server) {}