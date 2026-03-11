package com.app.infrastructure.adapter.plugin;
import com.app.infrastructure.adapter.logging.InfrastructureLogger;
import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
public class DefaultPluginContext implements PluginContext {
    private static final String APP_VERSION = "1.0.0";
    private static final Path DATA_DIR = Paths.get("data");
    private final Map<String, List<Consumer<Object>>> eventSubscribers = new HashMap<>();
    private final List<Plugin.ExportFormat> exportFormats = new ArrayList<>();
    @Override
    public String getAppVersion() {
        return APP_VERSION;
    }
    @Override
    public Path getDataDirectory() {
        return DATA_DIR;
    }
    @Override
    public Path getPluginDataDirectory(String pluginId) {
        Path dir = DATA_DIR.resolve("plugins").resolve(pluginId);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            InfrastructureLogger.logWarn("PluginContext", "Could not create plugin data dir: " + e.getMessage());
        }
        return dir;
    }
    @Override
    public void addMenuItem(MenuItem menuItem) {
        publish("PLUGIN_MENU_ADDED", menuItem);
    }
    @Override
    public void removeMenuItem(MenuItem menuItem) {
        publish("PLUGIN_MENU_REMOVED", menuItem);
    }
    @Override
    public void addPanel(String title, Pane panel) {
        publish("PLUGIN_PANEL_ADDED", new PanelRegistration(title, panel));
    }
    @Override
    public void removePanel(String title) {
        publish("PLUGIN_PANEL_REMOVED", title);
    }
    @Override
    public void showNotification(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    @Override
    public void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    @Override
    public void registerExportFormat(Plugin.ExportFormat format) {
        exportFormats.add(format);
    }
    @Override
    public void unregisterExportFormat(String formatId) {
        exportFormats.removeIf(f -> f.id().equals(formatId));
    }
    @Override
    public List<Plugin.ExportFormat> getRegisteredExportFormats() {
        return new ArrayList<>(exportFormats);
    }
    @Override
    public void subscribe(String eventType, Consumer<Object> handler) {
        eventSubscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }
    @Override
    public void unsubscribe(String eventType, Consumer<Object> handler) {
        List<Consumer<Object>> handlers = eventSubscribers.get(eventType);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }
    @Override
    public void publish(String eventType, Object data) {
        List<Consumer<Object>> handlers = eventSubscribers.get(eventType);
        if (handlers != null) {
            for (Consumer<Object> handler : handlers) {
                try {
                    handler.accept(data);
                } catch (Exception e) {
                    InfrastructureLogger.logError("PluginContext", "Error in event handler for: " + eventType, e);
                }
            }
        }
    }
    @Override
    public void logInfo(String message) {
        InfrastructureLogger.logInfo("Plugin", message);
    }
    @Override
    public void logWarning(String message) {
        InfrastructureLogger.logWarn("Plugin", message);
    }
    @Override
    public void logError(String message, Throwable error) {
        InfrastructureLogger.logError("Plugin", message, error);
    }
    public record PanelRegistration(String title, Pane panel) {}
}

