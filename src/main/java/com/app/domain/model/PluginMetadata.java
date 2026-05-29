package com.app.domain.model;
public record PluginMetadata(
    String id,
    String name,
    String version,
    String author,
    String description,
    boolean enabled,
    boolean isLoaded,
    String jarPath,
    String downloadUrl,
    PluginOrigin source
) {
    public PluginMetadata(String name) {
        this("builtin-" + name.toLowerCase().replace(" ", "-"), name, "1.0.0", 
             "System", "Built-in plugin", true, true, null, null, PluginOrigin.BUILTIN);
    }
    public PluginMetadata withEnabled(boolean enabled) {
        return new PluginMetadata(id, name, version, author, description, enabled, isLoaded, jarPath, downloadUrl, source);
    }
    public PluginMetadata withLoaded(boolean loaded) {
        return new PluginMetadata(id, name, version, author, description, enabled, loaded, jarPath, downloadUrl, source);
    }

    public PluginMetadata withDownloadUrl(String downloadUrl) {
        return new PluginMetadata(id, name, version, author, description, enabled, isLoaded, jarPath, downloadUrl, source);
    }

    public PluginMetadata withSource(PluginOrigin source) {
        return new PluginMetadata(id, name, version, author, description, enabled, isLoaded, jarPath, downloadUrl, source);
    }
}
