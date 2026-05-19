package com.app.infrastructure.sync;

import com.app.domain.model.EventPlanning;

public record EventPlanningConflict(EventPlanning local, EventPlanning server) {}