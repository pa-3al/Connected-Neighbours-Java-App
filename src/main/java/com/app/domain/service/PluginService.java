package com.app.domain.service;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.app.domain.model.PluginMetadata;
import com.app.domain.port.in.PluginUseCase;
import com.app.domain.port.out.I18nPort;
import com.app.domain.port.out.LoggerPort;
import com.app.domain.port.out.PluginRepository;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.config.ConfigProvider;
import com.app.infrastructure.sync.DesktopPluginBackendGateway;
import com.app.infrastructure.sync.DesktopPluginSqliteGateway;
import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;
public class PluginService implements PluginUseCase {
    private final PluginRepository pluginRepository;
    private final PluginContext pluginContext;
    private final I18nPort i18n;
    private final LoggerPort logger;
    private final DesktopPluginSqliteGateway desktopPluginSqliteGateway = new DesktopPluginSqliteGateway(new DatabaseConfig());
    private final DesktopPluginBackendGateway desktopPluginBackendGateway = new DesktopPluginBackendGateway(new ConfigProvider(), new AuthenticatedHttpClient());
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
    public List<PluginMetadata> getAvailablePlugins() {
        return desktopPluginSqliteGateway.loadDesktopPlugins();
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
        if (jarFile == null) {
            throw new IllegalArgumentException("Plugin JAR is required");
        }
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
    public void downloadPlugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin id is required");
        }

        PluginMetadata available = getAvailablePlugins().stream()
                .filter(plugin -> pluginId.equals(plugin.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(i18n.get("plugin.error.not_found", pluginId)));

        String downloadUrl = available.downloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            downloadUrl = desktopPluginBackendGateway.fetchPluginDownloadUrl(pluginId);
        }
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IllegalStateException("Missing plugin download URL for: " + pluginId);
        }

        Path tempJar = null;
        Path renamedJar = null;
        try {
            tempJar = downloadJar(downloadUrl, pluginId);
            renamedJar = tempJar.getParent().resolve(pluginId + ".jar");
            Files.move(tempJar, renamedJar, StandardCopyOption.REPLACE_EXISTING);
            tempJar = renamedJar;
            
            pluginRepository.installPlugin(renamedJar.toFile());
            desktopPluginSqliteGateway.updatePluginLoadedStatus(pluginId, true);
            loadPlugins();
        } catch (IOException e) {
            throw new RuntimeException("Failed to download plugin: " + pluginId, e);
        } finally {
            if (tempJar != null) {
                try {
                    Files.deleteIfExists(tempJar);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void uninstallPlugin(String pluginId) {
        logger.info("PluginService", "Uninstalling plugin: " + pluginId);

        PluginMetadata meta = pluginsMap.get(pluginId);
        if (meta != null && meta.jarPath() != null) {
            String fileName = new File(meta.jarPath()).getName();
            if (fileName.endsWith(".jar")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            try {
                java.util.UUID.fromString(fileName);
                desktopPluginSqliteGateway.updatePluginLoadedStatus(fileName, false);
            } catch (IllegalArgumentException ignored) {
            }
        }

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

    private Path downloadJar(String downloadUrl, String pluginId) throws IOException {
        validateUrl(downloadUrl);
        Path tempFile = Files.createTempFile("plugin-" + pluginId + "-", ".jar");
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Download failed with status: " + response.statusCode());
            }
            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Plugin download interrupted", e);
        }
    }

    private void validateUrl(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only HTTP/HTTPS URLs are allowed, got: " + scheme);
        }
    }
}
