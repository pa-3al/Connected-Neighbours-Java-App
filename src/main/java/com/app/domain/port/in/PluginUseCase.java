package com.app.domain.port.in;
import com.app.domain.model.PluginMetadata;
import java.util.List;
public interface PluginUseCase {
    List<PluginMetadata> loadPlugins();
    List<PluginMetadata> getInstalledPlugins();
    void enablePlugin(String pluginId);
    void disablePlugin(String pluginId);
    void uninstallPlugin(String pluginId);
    void reloadPlugin(String pluginId);
    void installPlugin(java.io.File file);
    boolean isPluginEnabled(String pluginId);
}
