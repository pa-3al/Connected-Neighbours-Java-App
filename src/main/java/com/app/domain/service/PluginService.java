package com.app.domain.service;
import com.app.domain.model.PluginMetadata;
import com.app.domain.port.in.PluginUseCase;
import com.app.domain.port.out.I18nPort;
import com.app.domain.port.out.LoggerPort;
import com.app.domain.port.out.PluginRepository;
import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class PluginService implements PluginUseCase {
    private final PluginRepository pluginRepository;
    private final PluginContext pluginContext;
    private final I18nPort i18n;
    private final LoggerPort logger;
    private final Map<String, PluginMetadata> pluginsMap = new HashMap<>();
    private final Map<String, Plugin> loadedPlugins = new HashMap<>();

    public PluginService(PluginRepository pluginRepository, PluginContext pluginContext,
                         I18nPort i18n, LoggerPort logger) {
        this.pluginRepository = pluginRepository;
        this.pluginContext = pluginContext;
        this.i18n = i18n;
        this.logger = logger;
    }

    @Override
    public List<PluginMetadata> loadPlugins() {
        List<PluginMetadata> discovered = new ArrayList<>(pluginRepository.discoverAndLoadPlugins());

        Map<String, Boolean> savedStates = pluginRepository.loadPluginStates();
        pluginsMap.clear();
        loadedPlugins.clear();

        for (PluginMetadata meta : discovered) {
            boolean enabled = savedStates.getOrDefault(meta.id(), meta.enabled());
            PluginMetadata updatedMeta = meta.withEnabled(enabled).withLoaded(false);
            pluginsMap.put(meta.id(), updatedMeta);

            if (enabled) {
                try {
                    Plugin plugin = pluginRepository.loadPlugin(meta.id());

                    if (plugin != null) {
                        try {
                            plugin.onLoad(pluginContext);
                            loadedPlugins.put(meta.id(), plugin);
                            pluginsMap.put(meta.id(), updatedMeta.withLoaded(true));
                            logger.info("PluginService", "Plugin loaded: " + meta.name());
                        } catch (Exception e) {
                            logger.error("PluginService", "Failed to initialize plugin: " + meta.id(), e);
                            disableBrokenPlugin(meta.id(), updatedMeta, plugin);
                        }
                    } else {
                        logger.warn("PluginService", "Plugin class not found for id: " + meta.id() + ". Disabling it.");
                        pluginsMap.put(meta.id(), updatedMeta.withEnabled(false).withLoaded(false));
                    }
                } catch (Exception e) {
                    logger.error("PluginService", "Failed to load plugin: " + meta.id(), e);
                    pluginsMap.put(meta.id(), updatedMeta.withEnabled(false).withLoaded(false));
                }
            }
        }
        saveCurrentStates();
        return new ArrayList<>(pluginsMap.values());
    }

    @Override
    public List<PluginMetadata> getInstalledPlugins() {
        if (pluginsMap.isEmpty()) {
            loadPlugins();
        }
        return new ArrayList<>(pluginsMap.values());
    }

    @Override
    public void enablePlugin(String pluginId) {
        PluginMetadata meta = pluginsMap.get(pluginId);
        if (meta == null) {
            throw new IllegalArgumentException(i18n.get("plugin.error.not_found", pluginId));
        }

        logger.info("PluginService", "Enabling plugin: " + pluginId);
        try {
            Plugin plugin = pluginRepository.loadPlugin(pluginId);

            if (plugin != null) {
                try {
                    plugin.onLoad(pluginContext);
                    loadedPlugins.put(pluginId, plugin);
                    pluginsMap.put(pluginId, meta.withEnabled(true).withLoaded(true));
                } catch (Exception e) {
                    logger.error("PluginService", "Failed to initialize plugin while enabling: " + pluginId, e);
                    disableBrokenPlugin(pluginId, meta.withEnabled(true), plugin);
                    throw new IllegalStateException("Plugin could not be initialized. Verify plugin resources and dependencies.", e);
                }
            } else {
                logger.warn("PluginService", "Plugin class not found while enabling: " + pluginId);
                pluginsMap.put(pluginId, meta.withEnabled(false).withLoaded(false));
                throw new IllegalStateException("Plugin class not found for id: " + pluginId);
            }
        } catch (Exception e) {
            logger.error("PluginService", "Failed to enable plugin: " + pluginId, e);
            pluginsMap.put(pluginId, meta.withEnabled(false).withLoaded(false));
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
        }
        saveCurrentStates();
    }

    @Override
    public void disablePlugin(String pluginId) {
        PluginMetadata meta = pluginsMap.get(pluginId);
        if (meta == null) {
            throw new IllegalArgumentException(i18n.get("plugin.error.not_found", pluginId));
        }

        logger.info("PluginService", "Disabling plugin: " + pluginId);
        Plugin plugin = loadedPlugins.remove(pluginId);
        if (plugin != null) {
            try {
                plugin.onUnload(pluginContext);
            } catch (Exception e) {
                logger.error("PluginService", "Error unloading plugin: " + pluginId, e);
            }
            pluginRepository.unloadPlugin(pluginId);
        }
        pluginsMap.put(pluginId, meta.withEnabled(false).withLoaded(false));
        saveCurrentStates();
    }

    @Override
    public boolean isPluginEnabled(String pluginId) {
        PluginMetadata meta = pluginsMap.get(pluginId);
        return meta != null && meta.enabled();
    }

    @Override
    public void installPlugin(File jarFile) {
        logger.info("PluginService", "Installing plugin from: " + jarFile.getName());
        pluginRepository.installPlugin(jarFile);
        List<PluginMetadata> loaded = loadPlugins();

        PluginMetadata installed = findPluginByJarName(loaded, jarFile.getName());
        if (installed == null) {
            throw new IllegalStateException("Plugin installed file was not discovered. Verify plugin.json and plugin class.");
        }
        if (!installed.isLoaded()) {
            throw new IllegalStateException("Plugin was installed but failed to initialize. Check plugin resources/dependencies.");
        }
    }

    @Override
    public void uninstallPlugin(String pluginId) {
        logger.info("PluginService", "Uninstalling plugin: " + pluginId);

        disablePlugin(pluginId);
        pluginRepository.deletePlugin(pluginId);
        pluginsMap.remove(pluginId);
        saveCurrentStates();
    }

    @Override
    public void reloadPlugin(String pluginId) {
        logger.info("PluginService", "Reloading plugin: " + pluginId);
        if (isPluginEnabled(pluginId)) {
            disablePlugin(pluginId);
        }
        enablePlugin(pluginId);
    }

    private void saveCurrentStates() {
        Map<String, Boolean> states = new HashMap<>();
        for (Map.Entry<String, PluginMetadata> entry : pluginsMap.entrySet()) {
            states.put(entry.getKey(), entry.getValue().enabled());
        }
        pluginRepository.savePluginStates(states);
    }

    private PluginMetadata findPluginByJarName(List<PluginMetadata> plugins, String jarFileName) {
        for (PluginMetadata plugin : plugins) {
            if (plugin.jarPath() == null || plugin.jarPath().isBlank()) {
                continue;
            }
            File path = new File(plugin.jarPath());
            if (jarFileName.equalsIgnoreCase(path.getName())) {
                return plugin;
            }
        }
        return null;
    }

    private void disableBrokenPlugin(String pluginId, PluginMetadata baseMeta, Plugin plugin) {
        try {
            if (plugin != null) {
                plugin.onUnload(pluginContext);
            }
        } catch (Exception e) {
            logger.warn("PluginService", "Error while rolling back failed plugin load: " + pluginId + " - " + e.getMessage());
        }
        loadedPlugins.remove(pluginId);
        pluginRepository.unloadPlugin(pluginId);
        pluginsMap.put(pluginId, baseMeta.withEnabled(false).withLoaded(false));
    }
}
