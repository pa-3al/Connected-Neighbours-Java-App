package com.app.domain.port.out;
import com.app.domain.model.PluginMetadata;
import com.app.plugin.Plugin;
import java.util.List;
import java.util.Map;
public interface PluginRepository {
    List<PluginMetadata> discoverAndLoadPlugins();
    List<PluginMetadata> getInstalledPlugins();
    Plugin loadPlugin(String pluginId);
    void unloadPlugin(String pluginId);
    void deletePlugin(String pluginId);
    void savePluginStates(Map<String, Boolean> states);
    Map<String, Boolean> loadPluginStates();
}
