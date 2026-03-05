package com.app.plugin;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
public interface PluginContext {
    String getAppVersion();
    Path getDataDirectory();
    Path getPluginDataDirectory(String pluginId);
    void addMenuItem(MenuItem menuItem);
    void removeMenuItem(MenuItem menuItem);
    void addPanel(String title, Pane panel);
    void removePanel(String title);
    void showNotification(String title, String message);
    void showError(String title, String message);
    void registerExportFormat(Plugin.ExportFormat format);
    void unregisterExportFormat(String formatId);
    List<Plugin.ExportFormat> getRegisteredExportFormats();
    void subscribe(String eventType, Consumer<Object> handler);
    void unsubscribe(String eventType, Consumer<Object> handler);
    void publish(String eventType, Object data);
    void logInfo(String message);
    void logWarning(String message);
    void logError(String message, Throwable error);
    default boolean isHeadless() { return false; }
}
