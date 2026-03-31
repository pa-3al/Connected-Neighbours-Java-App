package com.app.infrastructure.sync;

import com.app.domain.model.Incident;

public record IncidentConflict(Incident localIncident, Incident serverIncident) {
}
