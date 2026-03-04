package com.app.domain.port.in;
import java.util.List;
public interface QueryUseCase {
    List<String> executeQuery(String query);
}
