package com.app.infrastructure.adapter.plugin;
import com.app.infrastructure.adapter.logging.InfrastructureLogger;
import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
public class HeadlessPluginContext implements PluginContext {
    private final DefaultPluginContext delegate;
    public HeadlessPluginContext() {
        this.delegate = new DefaultPluginContext();
    }
    @Override
    public boolean isHeadless() {
        return true;
    }
    @Override
    public String getAppVersion() {
        return delegate.getAppVersion();
    }
    @Override
    public Path getDataDirectory() {
        return delegate.getDataDirectory();
    }
    @Override
    public Path getPluginDataDirectory(String pluginId) {
        return delegate.getPluginDataDirectory(pluginId);
    }
    @Override
    public void addMenuItem(MenuItem menuItem) {
    }
    @Override
    public void removeMenuItem(MenuItem menuItem) {
    }
    @Override
    public void addPanel(String title, Pane panel) {
    }
    @Override
    public void removePanel(String title) {
    }
    @Override
    public void showNotification(String title, String message) {
        InfrastructureLogger.logInfo("HeadlessPlugin", title + ": " + message);
    }
    @Override
    public void showError(String title, String message) {
        InfrastructureLogger.logError("HeadlessPlugin", title + ": " + message, null);
    }
    @Override
    public void registerExportFormat(Plugin.ExportFormat format) {
        delegate.registerExportFormat(format);
    }
    @Override
    public void unregisterExportFormat(String formatId) {
        delegate.unregisterExportFormat(formatId);
    }
    @Override
    public List<Plugin.ExportFormat> getRegisteredExportFormats() {
        return delegate.getRegisteredExportFormats();
    }
    @Override
    public void subscribe(String eventType, Consumer<Object> handler) {
        delegate.subscribe(eventType, handler);
    }
    @Override
    public void unsubscribe(String eventType, Consumer<Object> handler) {
        delegate.unsubscribe(eventType, handler);
    }
    @Override
    public void publish(String eventType, Object data) {
        delegate.publish(eventType, data);
    }
    @Override
    public void logInfo(String message) {
        delegate.logInfo(message);
    }
    @Override
    public void logWarning(String message) {
        delegate.logWarning(message);
    }
    @Override
    public void logError(String message, Throwable error) {
        delegate.logError(message, error);
    }
}

