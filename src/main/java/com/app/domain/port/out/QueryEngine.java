package com.app.domain.port.out;
import java.util.List;
public interface QueryEngine {
    List<String> parseAndRun(String query);
}
