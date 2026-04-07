package com.app.domain.service;

import com.app.domain.port.out.QueryEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private QueryEngine queryEngine;

    private QueryService service;

    @BeforeEach
    void setUp() {
        service = new QueryService(queryEngine);
    }

    @Test
    void executeQueryShouldReturnResultsForValidQuery() {
        when(queryEngine.parseAndRun("SELECT *")).thenReturn(List.of("Result 1"));
        List<String> results = service.executeQuery("SELECT *");
        assertEquals(1, results.size());
        assertEquals("Result 1", results.get(0));
    }

    @Test
    void executeQueryShouldReturnEmptyQueryMessageForNull() {
        List<String> results = service.executeQuery(null);
        assertEquals(1, results.size());
        assertEquals("Empty Query", results.get(0));
        verifyNoInteractions(queryEngine);
    }

    @Test
    void executeQueryShouldReturnEmptyQueryMessageForBlank() {
        List<String> results = service.executeQuery("   ");
        assertEquals(1, results.size());
        assertEquals("Empty Query", results.get(0));
        verifyNoInteractions(queryEngine);
    }
}