package com.app.infrastructure.adapter.query;
import com.app.domain.port.out.QueryEngine;
import com.app.query.QueryLexer;
import com.app.query.QueryParser;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
public class JFlexQueryAdapter implements QueryEngine {
    @Override
    public List<String> parseAndRun(String query) {
        try {
            String input = query.trim().endsWith(";") ? query : query + ";";
            QueryLexer lexer = new QueryLexer(new StringReader(input));
            QueryParser parser = new QueryParser(lexer, new java_cup.runtime.ComplexSymbolFactory());
            parser.parse();
            return parser.result;
        } catch (Exception e) {
            return Collections.singletonList("Error executing query: " + e.getMessage());
        } catch (Error e) {
             return Collections.singletonList("Lexical Error: " + e.getMessage());
        }
    }
}
