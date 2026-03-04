package com.app.domain.port.in;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentStatus;
import java.util.List;

public interface IncidentUseCase {
    List<Incident> getAllIncidents();
    Incident getIncidentById(String id);
    Incident createIncident(Incident incident);
    Incident updateIncident(Incident incident);
    Incident resolveIncident(String id);
    void deleteIncident(String id);
    List<Incident> getIncidentsByStatus(IncidentStatus status);
}
