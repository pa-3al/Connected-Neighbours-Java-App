package com.app.domain.service;

import com.app.domain.model.Alert;
import com.app.domain.port.in.AlertUseCase;
import com.app.domain.port.out.AlertRepository;
import java.util.List;

public class AlertService implements AlertUseCase {

    private final AlertRepository repository;

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Alert> getAllAlerts() {
        return repository.findAll();
    }

    @Override
    public List<Alert> getActiveAlerts() {
        return repository.findActive();
    }

    @Override
    public Alert createAlert(Alert alert) {
        return repository.save(alert);
    }

    @Override
    public Alert deactivateAlert(String id) {
        return repository.findById(id)
            .map(alert -> {
                Alert deactivated = alert.withActive(false);
                return repository.save(deactivated);
            })
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
    }

    @Override
    public void deleteAlert(String id) {
        repository.deleteById(id);
    }
}
