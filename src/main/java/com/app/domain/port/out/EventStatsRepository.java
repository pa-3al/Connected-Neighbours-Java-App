package com.app.domain.port.out;

import java.util.Map;

public interface EventStatsRepository {
    Map<String, Integer> getEventsByMonth();
    Map<String, Integer> getTopEventsByParticipation();
    Map<String, Integer> getEventsByPriceType();
    Map<String, Integer> getEventPriceDistribution();
    Map<String, Double> getAveragePriceByMonth();
}