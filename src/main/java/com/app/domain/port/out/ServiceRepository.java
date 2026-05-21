package com.app.domain.port.out;

import java.util.Map;

public interface ServiceRepository {
    Map<String, Integer> countServicesByStatus();
    Map<String, Integer> countExpectedDatesByMonth();

}
