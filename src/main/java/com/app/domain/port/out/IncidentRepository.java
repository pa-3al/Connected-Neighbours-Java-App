package com.app.domain.port.out;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentStatus;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository {
    List<Incident> findAll();
    Optional<Incident> findById(String id);
    Incident save(Incident incident);
    void deleteById(String id);
    List<Incident> findByStatus(IncidentStatus status);
}
