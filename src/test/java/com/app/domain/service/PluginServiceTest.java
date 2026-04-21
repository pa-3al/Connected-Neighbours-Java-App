package com.app.domain.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.app.domain.model.PluginMetadata;
import com.app.domain.port.out.I18nPort;
import com.app.domain.port.out.LoggerPort;
import com.app.domain.port.out.PluginRepository;
import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;

class PluginServiceTest {
    private static final int EXPECTED_PLUGIN_COUNT = 1;

    private PluginService pluginService;
    private MockPluginRepository mockRepo;
    private MockPluginContext mockContext;
    private MockI18nPort mockI18n;
    private MockLoggerPort mockLogger;

    @BeforeEach
    void setUp() {
        mockRepo = new MockPluginRepository();
        mockContext = new MockPluginContext();
        mockI18n = new MockI18nPort();
        mockLogger = new MockLoggerPort();
        pluginService = new PluginService(mockRepo, mockContext, mockI18n, mockLogger);
    }
    @Test
    void testEnablePlugin_shouldThrow_whenNotFound() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> pluginService.enablePlugin("non-existent-plugin")
        );
        assertTrue(ex.getMessage().contains("plugin.error.not_found"));
    }
    @Test
    void testDisablePlugin_shouldThrow_whenNotFound() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> pluginService.disablePlugin("non-existent-plugin")
        );
        assertTrue(ex.getMessage().contains("plugin.error.not_found"));
    }
    @Test
    void testIsPluginEnabled_shouldReturnCorrectState() {
        pluginService.loadPlugins();
        assertTrue(pluginService.isPluginEnabled("test-plugin"));
        pluginService.disablePlugin("test-plugin");
        assertFalse(pluginService.isPluginEnabled("test-plugin"));
        pluginService.enablePlugin("test-plugin");
        assertTrue(pluginService.isPluginEnabled("test-plugin"));
    }
    @Test
    void testLoadPlugins_shouldReturnDiscoveredPlugins() {
        List<PluginMetadata> plugins = pluginService.loadPlugins();
        assertNotNull(plugins);
        assertFalse(plugins.isEmpty());
        assertEquals(EXPECTED_PLUGIN_COUNT, plugins.size());
        assertTrue(plugins.stream().anyMatch(p -> "test-plugin".equals(p.id())));
    }
    @Test
    void testGetInstalledPlugins_shouldLoadIfEmpty() {
        List<PluginMetadata> plugins = pluginService.getInstalledPlugins();
        assertNotNull(plugins);
        assertEquals(EXPECTED_PLUGIN_COUNT, plugins.size());
    }
    @Test
    void testUninstallPlugin_shouldRemovePlugin() {
        pluginService.loadPlugins();
        assertTrue(pluginService.isPluginEnabled("test-plugin"));
        pluginService.uninstallPlugin("test-plugin");
        assertFalse(pluginService.isPluginEnabled("test-plugin"));
        assertTrue(mockRepo.deletedPlugins.contains("test-plugin"));
    }
    @Test
    void testReloadPlugin_shouldDisableAndReEnable() {
        pluginService.loadPlugins();
        pluginService.reloadPlugin("test-plugin");
        assertTrue(pluginService.isPluginEnabled("test-plugin"));
    }

    @Test
    void testInstallPlugin_shouldDelegateToRepository() {
        File jarFile = new File("my-plugin.jar");

        pluginService.installPlugin(jarFile);

        assertEquals(1, mockRepo.installedPluginFiles.size());
        assertEquals("my-plugin.jar", mockRepo.installedPluginFiles.get(0).getName());
    }

    private static class MockPluginRepository implements PluginRepository {
        List<String> deletedPlugins = new ArrayList<>();
        List<File> installedPluginFiles = new ArrayList<>();
        @Override
        public List<PluginMetadata> discoverAndLoadPlugins() {
            return new ArrayList<>(List.of(
                new PluginMetadata("test-plugin", "Test Plugin", "1.0.0", "Test Author", 
                    "A test plugin", true, false, null)
            ));
        }
        @Override
        public List<PluginMetadata> getInstalledPlugins() {
            return discoverAndLoadPlugins();
        }
        @Override
        public void installPlugin(java.io.File jarFile) {
        }
        @Override
        public Plugin loadPlugin(String pluginId) {
            return new MockPlugin();
        }
        @Override
        public void unloadPlugin(String pluginId) {}
        @Override
        public void deletePlugin(String pluginId) {
            deletedPlugins.add(pluginId);
        }
        @Override
        public void savePluginStates(Map<String, Boolean> states) {
        }
        @Override
        public Map<String, Boolean> loadPluginStates() {
            return new HashMap<>();
        }
    }
    private static class MockPluginContext implements PluginContext {
        @Override public String getAppVersion() { return "1.0.0"; }
        @Override public Path getDataDirectory() { return Paths.get("data"); }
        @Override public Path getPluginDataDirectory(String pluginId) { return Paths.get("data", "plugins", pluginId); }
        @Override public void addMenuItem(MenuItem menuItem) {}
        @Override public void removeMenuItem(MenuItem menuItem) {}
        @Override public void addPanel(String title, Pane panel) {}
        @Override public void removePanel(String title) {}
        @Override public void showNotification(String title, String message) {}
        @Override public void showError(String title, String message) {}
        @Override public void registerExportFormat(Plugin.ExportFormat format) {}
        @Override public void unregisterExportFormat(String formatId) {}
        @Override public List<Plugin.ExportFormat> getRegisteredExportFormats() { return Collections.emptyList(); }
        @Override public void subscribe(String eventType, Consumer<Object> handler) {}
        @Override public void unsubscribe(String eventType, Consumer<Object> handler) {}
        @Override public void publish(String eventType, Object data) {}
        @Override public void logInfo(String message) {}
        @Override public void logWarning(String message) {}
        @Override public void logError(String message, Throwable error) {}
        @Override public boolean isHeadless() { return true; }
    }
    private static class MockPlugin implements Plugin {
        @Override public String getId() { return "test-plugin"; }
        @Override public String getName() { return "Test Plugin"; }
        @Override public String getVersion() { return "1.0.0"; }
        @Override public void onLoad(PluginContext context) {}
        @Override public void onUnload(PluginContext context) {}
        @Override public Node getSettingsPanel() { return null; }
        @Override public List<MenuItem> getMenuItems() { return Collections.emptyList(); }
        @Override public List<ExportFormat> getExportFormats() { return Collections.emptyList(); }
    }

    private static class MockI18nPort implements I18nPort {
        @Override
        public String get(String key, Object... args) {
            return key;
        }
    }

    private static class MockLoggerPort implements LoggerPort {
        @Override public void debug(String tag, String message) {}
        @Override public void info(String tag, String message) {}
        @Override public void warn(String tag, String message) {}
        @Override public void error(String tag, String message, Throwable throwable) {}
    }
}
