package com.app.domain.port.in;

import com.app.domain.model.Alert;
import java.util.List;

public interface AlertUseCase {
    List<Alert> getAllAlerts();
    List<Alert> getActiveAlerts();
    Alert createAlert(Alert alert);
    Alert deactivateAlert(String id);
    void deleteAlert(String id);
}
