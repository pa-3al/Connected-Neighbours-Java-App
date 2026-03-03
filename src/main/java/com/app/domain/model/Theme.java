package com.app.domain.model;
public record Theme(
    String id,
    String name,
    String author,
    String description,
    String cssPath,
    boolean isBuiltIn
) {
    public static Theme builtIn(String id, String name, String cssPath) {
        return new Theme(id, name, "System", "Built-in theme", cssPath, true);
    }
    public static Theme custom(String id, String name, String author, String description, String cssPath) {
        return new Theme(id, name, author, description, cssPath, false);
    }
}
