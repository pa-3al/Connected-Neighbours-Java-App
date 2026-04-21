package com.app.domain.port.out;
import java.io.File;
import java.util.List;
import java.util.Map;

import com.app.domain.model.PluginMetadata;
import com.app.plugin.Plugin;
public interface PluginRepository {
    List<PluginMetadata> discoverAndLoadPlugins();
    List<PluginMetadata> getInstalledPlugins();
    void installPlugin(File jarFile);
    Plugin loadPlugin(String pluginId);
    void unloadPlugin(String pluginId);
    void deletePlugin(String pluginId);
    void savePluginStates(Map<String, Boolean> states);
    Map<String, Boolean> loadPluginStates();
}
