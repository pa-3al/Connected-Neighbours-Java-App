package com.app.infrastructure.ui;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.app.domain.model.PluginMetadata;
import com.app.domain.model.PluginOrigin;
import com.app.domain.port.in.PluginUseCase;
import com.app.infrastructure.util.KeyboardShortcutsHandler;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
public class PluginController {
    private final com.app.infrastructure.i18n.I18nService i18n = com.app.infrastructure.i18n.I18nService.getInstance();
    private final PluginUseCase pluginUseCase;
    @FXML private VBox pluginRoot;
    @FXML private FlowPane pluginsContainer;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    public PluginController(PluginUseCase pluginUseCase) {
        this.pluginUseCase = pluginUseCase;
    }
    @FXML
    public void initialize() {
        refreshPluginsList();
        KeyboardShortcutsHandler.registerContext(pluginRoot, this::handleShortcut);
    }

    private boolean handleShortcut(KeyboardShortcutsHandler.ShortcutAction action, Node focusOwner) {
        if (focusOwner == null) {
            return false;
        }
        return switch (action) {
            case NEW_ITEM -> {
                handleAddPlugin();
                yield true;
            }
            case REFRESH -> {
                handleRefreshPlugins();
                yield true;
            }
            default -> false;
        };
    }
    @FXML
    private void handleRefreshPlugins() {
        refreshPluginsList();
        setStatus(i18n.get("status.success", i18n.get("plugin.status.refreshed")));
    }

    @FXML
    private void handleAddPlugin() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(i18n.get("plugin.dialog.install.title"));
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(i18n.get("file.filter.jar"), "*.jar"));
        java.io.File file = fileChooser.showOpenDialog(pluginsContainer.getScene().getWindow());
        if (file != null) {
            try {
                pluginUseCase.installPlugin(file);
                refreshPluginsList();
                setStatus(i18n.get("status.success", i18n.get("plugin.status.installed.success", file.getName())));
            } catch (Exception e) {
                setStatus(i18n.get("status.error", i18n.get("plugin.status.installed.error", e.getMessage())));
            }
        }
    }

    @FXML
    private void handleOpenGuide() {
        try {
            java.io.File guide = new java.io.File("PLUGIN_DEVELOPMENT.md");
            if (guide.exists()) {
                java.awt.Desktop.getDesktop().open(guide);
            } else {
                setStatus(i18n.get("status.error", i18n.get("plugin.status.guide.missing")));
            }
        } catch (Exception e) {
            setStatus(i18n.get("status.error", i18n.get("plugin.status.guide.error")));
        }
    }
    private void refreshPluginsList() {
        pluginsContainer.getChildren().clear();
        List<PluginMetadata> plugins = mergePlugins(pluginUseCase.getInstalledPlugins(), pluginUseCase.getAvailablePlugins());
        countLabel.textProperty().bind(i18n.createStringBinding("plugin.count", plugins.size()));
        if (plugins.isEmpty()) {
            Label emptyLabel = new Label();
            emptyLabel.textProperty().bind(i18n.createStringBinding("plugin.empty"));
            emptyLabel.getStyleClass().add("empty-message");
            pluginsContainer.getChildren().add(emptyLabel);
            return;
        }
        for (PluginMetadata plugin : plugins) {
            VBox card = createPluginCard(plugin);
            pluginsContainer.getChildren().add(card);
        }
    }
    private VBox createPluginCard(PluginMetadata plugin) {
        VBox card = new VBox(8);
        card.getStyleClass().add("plugin-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(280);
        card.setAlignment(Pos.TOP_LEFT);
        if (plugin.enabled() && plugin.isLoaded()) {
            card.getStyleClass().add("plugin-card-active");
        } else if (!plugin.enabled()) {
            card.getStyleClass().add("plugin-card-disabled");
        }
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(plugin.name());
        nameLabel.getStyleClass().add("plugin-card-title");
        Label versionBadge = new Label();
        versionBadge.textProperty().bind(i18n.createStringBinding("plugin.badge.version", plugin.version()));
        versionBadge.getStyleClass().add("version-badge");
        header.getChildren().addAll(nameLabel, versionBadge);
        Label authorLabel = new Label();
        authorLabel.textProperty().bind(i18n.createStringBinding("plugin.badge.author", plugin.author()));
        authorLabel.getStyleClass().add("plugin-card-author");
        Label descLabel = new Label(plugin.description());
        descLabel.getStyleClass().add("plugin-card-description");
        descLabel.setWrapText(true);
        
        Label statusBadge = new Label();
        if (plugin.source() == PluginOrigin.REMOTE_CATALOG && (plugin.jarPath() == null || plugin.jarPath().isBlank())) {
              statusBadge.setText("Available on the server");
        } else if (plugin.enabled()) {
             statusBadge.textProperty().bind(plugin.isLoaded() ? i18n.createStringBinding("plugin.status.enabled") : i18n.createStringBinding("plugin.status.enabled.unloaded"));
        } else {
             statusBadge.textProperty().bind(i18n.createStringBinding("plugin.status.disabled"));
        }

        statusBadge.getStyleClass().add("status-badge");
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        if (plugin.source() == PluginOrigin.REMOTE_CATALOG && (plugin.jarPath() == null || plugin.jarPath().isBlank())) {
            Button downloadBtn = new Button();
            downloadBtn.setText("Download");
            downloadBtn.getStyleClass().add("success-button");
            downloadBtn.setOnAction(e -> handleDownloadPlugin(plugin));
            buttons.getChildren().add(downloadBtn);
        } else {
            Button toggleBtn = new Button();
            toggleBtn.textProperty().bind(plugin.enabled() ? i18n.createStringBinding("plugin.action.disable") : i18n.createStringBinding("plugin.action.enable"));
            toggleBtn.getStyleClass().add(plugin.enabled() ? "warning-button" : "success-button");
            toggleBtn.setOnAction(e -> handleTogglePlugin(plugin));
            buttons.getChildren().add(toggleBtn);
        }

        Label sourceBadge = new Label();
        if (plugin.source() == PluginOrigin.BUILTIN) {
            sourceBadge.textProperty().bind(i18n.createStringBinding("plugin.badge.builtin"));
            sourceBadge.getStyleClass().add("badge-builtin");
        } else if (plugin.source() == PluginOrigin.REMOTE_CATALOG) {
            sourceBadge.setText("Server");
            sourceBadge.getStyleClass().add("badge-server");
        } else {
            sourceBadge.setText("Local");
            sourceBadge.getStyleClass().add("badge-local");
        }
        buttons.getChildren().add(sourceBadge);

        if (plugin.jarPath() != null) {
            Button uninstallBtn = new Button();
            uninstallBtn.textProperty().bind(i18n.createStringBinding("plugin.action.uninstall"));
            uninstallBtn.getStyleClass().add("danger-button");
            uninstallBtn.setOnAction(e -> handleUninstallPlugin(plugin));
            buttons.getChildren().add(uninstallBtn);
        }
        card.getChildren().addAll(header, authorLabel, descLabel, statusBadge, buttons);
        return card;
    }

    private void handleDownloadPlugin(PluginMetadata plugin) {
        try {
            pluginUseCase.downloadPlugin(plugin.id());
            refreshPluginsList();
            setStatus(i18n.get("status.success", i18n.get("plugin.status.installed.success", plugin.name())));
        } catch (Exception e) {
            setStatus(i18n.get("status.error", i18n.get("plugin.status.installed.error", e.getMessage())));
        }
    }
    private void handleTogglePlugin(PluginMetadata plugin) {
        try {
            boolean wasEnabled = plugin.enabled();
            String actionKey = wasEnabled ? "plugin.action.deactivated" : "plugin.action.activated";
            String action = i18n.get(actionKey);
            
            if (wasEnabled) {
                com.app.infrastructure.util.DailyLogger.logInfo("Plugins", "User disabling plugin: " + plugin.name() + " (" + plugin.id() + ")");
                pluginUseCase.disablePlugin(plugin.id());
            } else {
                com.app.infrastructure.util.DailyLogger.logInfo("Plugins", "User enabling plugin: " + plugin.name() + " (" + plugin.id() + ")");
                pluginUseCase.enablePlugin(plugin.id());
            }
            refreshPluginsList();
            setStatus(i18n.get("plugin.status.toggled", plugin.name(), action));
        } catch (Exception e) {
            com.app.infrastructure.util.DailyLogger.logError("Plugins", "Failed to toggle plugin " + plugin.id(), e);
            setStatus(i18n.get("status.error", i18n.get("error.detail", e.getMessage())));
        }
    }
    private void handleUninstallPlugin(PluginMetadata plugin) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.titleProperty().bind(i18n.createStringBinding("plugin.dialog.uninstall.title"));
        confirm.headerTextProperty().bind(i18n.createStringBinding("plugin.dialog.uninstall.header", plugin.name()));
        confirm.contentTextProperty().bind(i18n.createStringBinding("plugin.dialog.uninstall.content"));
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                com.app.infrastructure.util.DailyLogger.logWarn("Plugins", "User uninstalling plugin: " + plugin.name() + " (" + plugin.id() + ")");
                pluginUseCase.uninstallPlugin(plugin.id());
                refreshPluginsList();
                setStatus(i18n.get("status.success", i18n.get("plugin.status.uninstalled.success")));
            } catch (Exception e) {
                com.app.infrastructure.util.DailyLogger.logError("Plugins", "Failed to uninstall plugin " + plugin.id(), e);
                setStatus(i18n.get("status.error", e.getMessage()));
            }
        }
    }
    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private List<PluginMetadata> mergePlugins(List<PluginMetadata> installed, List<PluginMetadata> available) {
        Map<String, PluginMetadata> merged = new LinkedHashMap<>();
        for (PluginMetadata plugin : installed) {
            merged.put(plugin.id(), plugin);
        }
        for (PluginMetadata plugin : available) {
            merged.merge(plugin.id(), plugin, this::mergeCatalogIntoInstalled);
        }
        return new ArrayList<>(merged.values());
    }

    private PluginMetadata mergeCatalogIntoInstalled(PluginMetadata installed, PluginMetadata catalog) {
        return new PluginMetadata(
                installed.id(),
                installed.name() != null && !installed.name().isBlank() ? installed.name() : catalog.name(),
                installed.version() != null && !installed.version().isBlank() ? installed.version() : catalog.version(),
                installed.author() != null && !installed.author().isBlank() ? installed.author() : catalog.author(),
                installed.description() != null && !installed.description().isBlank() ? installed.description() : catalog.description(),
                installed.enabled(),
                installed.isLoaded(),
                installed.jarPath() != null ? installed.jarPath() : catalog.jarPath(),
                catalog.downloadUrl() != null && !catalog.downloadUrl().isBlank() ? catalog.downloadUrl() : installed.downloadUrl(),
                catalog.source() != null ? catalog.source() : installed.source()
        );
    }
}
