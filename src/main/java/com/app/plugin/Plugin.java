package com.app.plugin;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import java.util.Collections;
import java.util.List;
public interface Plugin {
    String getId();
    String getName();
    String getVersion();
    default String getDescription() {
        return "";
    }
    default String getAuthor() {
        return "Unknown";
    }
    void onLoad(PluginContext context);
    void onUnload(PluginContext context);
    default Node getSettingsPanel() {
        return null;
    }
    default List<MenuItem> getMenuItems() {
        return Collections.emptyList();
    }
    default List<ExportFormat> getExportFormats() {
        return Collections.emptyList();
    }
    record ExportFormat(String id, String name, String extension, ExportHandler handler) {}
    @FunctionalInterface
    interface ExportHandler {
        void export(Object data, java.nio.file.Path destination) throws Exception;
    }
}
