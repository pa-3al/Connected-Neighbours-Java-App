package com.app.domain.service;

import com.app.domain.model.Alert;
import com.app.domain.port.out.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository repository;

    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(repository);
    }

    @Test
    void getAllAlertsShouldReturnListOfAlerts() {
        when(repository.findAll()).thenReturn(List.of(mock(Alert.class)));
        List<Alert> results = service.getAllAlerts();
        assertFalse(results.isEmpty());
    }

    @Test
    void getActiveAlertsShouldReturnActiveAlerts() {
        when(repository.findActive()).thenReturn(List.of(mock(Alert.class)));
        List<Alert> results = service.getActiveAlerts();
        assertFalse(results.isEmpty());
    }

    @Test
    void createAlertShouldReturnSavedAlert() {
        Alert mockAlert = mock(Alert.class);
        when(repository.save(mockAlert)).thenReturn(mockAlert);
        Alert result = service.createAlert(mockAlert);
        assertNotNull(result);
        verify(repository).save(mockAlert);
    }

    @Test
    void deactivateAlertShouldReturnDeactivatedAlertWhenFound() {
        Alert mockAlert = mock(Alert.class);
        Alert deactivatedAlert = mock(Alert.class);
        when(repository.findById("123")).thenReturn(Optional.of(mockAlert));
        when(mockAlert.withActive(false)).thenReturn(deactivatedAlert);
        when(repository.save(deactivatedAlert)).thenReturn(deactivatedAlert);
        Alert result = service.deactivateAlert("123");
        assertNotNull(result);
    }

    @Test
    void deactivateAlertShouldThrowExceptionWhenNotFound() {
        when(repository.findById("123")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.deactivateAlert("123"));
    }

    @Test
    void deleteAlertShouldCallRepositoryDelete() {
        service.deleteAlert("123");
        verify(repository).deleteById("123");
    }
}