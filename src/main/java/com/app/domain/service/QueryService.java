package com.app.domain.service;
import com.app.domain.port.in.QueryUseCase;
import com.app.domain.port.out.QueryEngine;
import java.util.List;
public class QueryService implements QueryUseCase {
    private final QueryEngine queryEngine;
    public QueryService(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }
    @Override
    public List<String> executeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of("Empty Query");
        }
        return queryEngine.parseAndRun(query);
    }
}
