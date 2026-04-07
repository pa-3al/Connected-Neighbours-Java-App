package com.app.domain.service;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.port.out.IncidentRepository;
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
class IncidentServiceTest {

    @Mock
    private IncidentRepository repository;

    private IncidentService service;

    @BeforeEach
    void setUp() {
        service = new IncidentService(repository);
    }

    @Test
    void getAllIncidentsShouldReturnListOfIncidents() {
        when(repository.findAll()).thenReturn(List.of(mock(Incident.class)));
        List<Incident> results = service.getAllIncidents();
        assertFalse(results.isEmpty());
    }

    @Test
    void getIncidentByIdShouldReturnIncidentWhenFound() {
        Incident mockIncident = mock(Incident.class);
        when(repository.findById("123")).thenReturn(Optional.of(mockIncident));
        Incident result = service.getIncidentById("123");
        assertNotNull(result);
    }

    @Test
    void getIncidentByIdShouldReturnNullWhenNotFound() {
        when(repository.findById("123")).thenReturn(Optional.empty());
        Incident result = service.getIncidentById("123");
        assertNull(result);
    }

    @Test
    void createIncidentShouldReturnSavedIncident() {
        Incident mockIncident = mock(Incident.class);
        when(mockIncident.id()).thenReturn("123");
        when(repository.save(mockIncident)).thenReturn(mockIncident);
        Incident result = service.createIncident(mockIncident);
        assertNotNull(result);
        verify(repository).save(mockIncident);
    }

    @Test
    void updateIncidentShouldReturnSavedIncident() {
        Incident mockIncident = mock(Incident.class);
        when(repository.save(mockIncident)).thenReturn(mockIncident);
        Incident result = service.updateIncident(mockIncident);
        assertNotNull(result);
        verify(repository).save(mockIncident);
    }

    @Test
    void resolveIncidentShouldReturnResolvedIncidentWhenFound() {
        Incident mockIncident = mock(Incident.class);
        Incident resolvedIncident = mock(Incident.class);
        when(repository.findById("123")).thenReturn(Optional.of(mockIncident));
        when(mockIncident.withStatus(IncidentStatus.COMPLETED)).thenReturn(resolvedIncident);
        when(repository.save(resolvedIncident)).thenReturn(resolvedIncident);
        Incident result = service.resolveIncident("123");
        assertNotNull(result);
    }

    @Test
    void resolveIncidentShouldThrowExceptionWhenNotFound() {
        when(repository.findById("123")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.resolveIncident("123"));
    }

    @Test
    void deleteIncidentShouldCallRepositoryDelete() {
        service.deleteIncident("123");
        verify(repository).deleteById("123");
    }

    @Test
    void getIncidentsByStatusShouldReturnFilteredList() {
        when(repository.findByStatus(IncidentStatus.COMPLETED)).thenReturn(List.of(mock(Incident.class)));
        List<Incident> results = service.getIncidentsByStatus(IncidentStatus.COMPLETED);
        assertFalse(results.isEmpty());
    }
}