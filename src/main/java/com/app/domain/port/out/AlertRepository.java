package com.app.domain.port.out;

import com.app.domain.model.Alert;
import java.util.List;
import java.util.Optional;

public interface AlertRepository {
    List<Alert> findAll();
    List<Alert> findActive();
    Optional<Alert> findById(String id);
    Alert save(Alert alert);
    void deleteById(String id);
}
