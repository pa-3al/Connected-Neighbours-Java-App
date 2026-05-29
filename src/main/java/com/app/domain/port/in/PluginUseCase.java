package com.app.domain.port.in;
import com.app.domain.model.PluginMetadata;
import java.util.List;
public interface PluginUseCase {
    List<PluginMetadata> loadPlugins();
    List<PluginMetadata> getInstalledPlugins();
    List<PluginMetadata> getAvailablePlugins();
    void enablePlugin(String pluginId);
    void disablePlugin(String pluginId);
    void uninstallPlugin(String pluginId);
    void reloadPlugin(String pluginId);
    void installPlugin(java.io.File file);
    void downloadPlugin(String pluginId);
    boolean isPluginEnabled(String pluginId);
}
