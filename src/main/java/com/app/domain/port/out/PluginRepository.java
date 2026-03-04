package com.app.domain.port.out;
import com.app.domain.model.PluginMetadata;
import java.util.List;
import java.util.Map;
public interface PluginRepository {
    List<PluginMetadata> discoverAndLoadPlugins();
    List<PluginMetadata> getInstalledPlugins();
    PluginMetadata loadPlugin(String pluginId);
    void unloadPlugin(String pluginId);
    void deletePlugin(String pluginId);
    void savePluginStates(Map<String, Boolean> states);
    Map<String, Boolean> loadPluginStates();
}
