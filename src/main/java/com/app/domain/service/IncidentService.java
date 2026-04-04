package com.app.domain.service;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.port.in.IncidentUseCase;
import com.app.domain.port.out.IncidentRepository;
import java.util.List;

public class IncidentService implements IncidentUseCase {

    private final IncidentRepository repository;

    public IncidentService(IncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Incident> getAllIncidents() {
        return repository.findAll();
    }

    @Override
    public Incident getIncidentById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Incident createIncident(Incident incident) {
        if (incident.id() == null || incident.id().isEmpty()) {
        }
        return repository.save(incident);
    }

    @Override
    public Incident updateIncident(Incident incident) {
        return repository.save(incident);
    }

    @Override
    public Incident resolveIncident(String id) {
        return repository.findById(id)
            .map(incident -> {
                Incident resolved = incident.withStatus(IncidentStatus.COMPLETED);
                return repository.save(resolved);
            })
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
    }

    @Override
    public void deleteIncident(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<Incident> getIncidentsByStatus(IncidentStatus status) {
        return repository.findByStatus(status);
    }
}
