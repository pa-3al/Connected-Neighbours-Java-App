package com.app.infrastructure.ui;
import com.app.domain.model.PluginMetadata;
import com.app.domain.port.in.PluginUseCase;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;
public class PluginController {
    private final com.app.infrastructure.i18n.I18nService i18n = com.app.infrastructure.i18n.I18nService.getInstance();
    private final PluginUseCase pluginUseCase;
    @FXML private FlowPane pluginsContainer;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    public PluginController(PluginUseCase pluginUseCase) {
        this.pluginUseCase = pluginUseCase;
    }
    @FXML
    public void initialize() {
        refreshPluginsList();
    }
    @FXML
    private void handleRefreshPlugins() {
        refreshPluginsList();
        setStatus("✅ " + i18n.get("plugin.status.refreshed"));
    }

    @FXML
    private void handleAddPlugin() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(i18n.get("plugin.dialog.install.title"));
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Java Archive", "*.jar"));
        java.io.File file = fileChooser.showOpenDialog(pluginsContainer.getScene().getWindow());
        if (file != null) {
            try {
                pluginUseCase.installPlugin(file);
                refreshPluginsList();
                setStatus("✅ " + i18n.get("plugin.status.installed.success", file.getName()));
            } catch (Exception e) {
                setStatus("❌ " + i18n.get("plugin.status.installed.error", e.getMessage()));
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
                setStatus("❌ " + i18n.get("plugin.status.guide.missing"));
            }
        } catch (Exception e) {
            setStatus("❌ " + i18n.get("plugin.status.guide.error"));
        }
    }
    private void refreshPluginsList() {
        pluginsContainer.getChildren().clear();
        List<PluginMetadata> plugins = pluginUseCase.getInstalledPlugins();
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
        if (plugin.enabled()) {
             statusBadge.textProperty().bind(plugin.isLoaded() ? i18n.createStringBinding("plugin.status.enabled") : i18n.createStringBinding("plugin.status.enabled.unloaded"));
        } else {
             statusBadge.textProperty().bind(i18n.createStringBinding("plugin.status.disabled"));
        }

        statusBadge.getStyleClass().add("status-badge");
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        Button toggleBtn = new Button();
        toggleBtn.textProperty().bind(plugin.enabled() ? i18n.createStringBinding("plugin.action.disable") : i18n.createStringBinding("plugin.action.enable"));
        toggleBtn.getStyleClass().add(plugin.enabled() ? "warning-button" : "success-button");
        toggleBtn.setOnAction(e -> handleTogglePlugin(plugin));
        buttons.getChildren().add(toggleBtn);
        if (plugin.jarPath() != null) {
            Button uninstallBtn = new Button();
            uninstallBtn.textProperty().bind(i18n.createStringBinding("plugin.action.uninstall"));
            uninstallBtn.getStyleClass().add("danger-button");
            uninstallBtn.setOnAction(e -> handleUninstallPlugin(plugin));
            buttons.getChildren().add(uninstallBtn);
        } else {
            Label builtinBadge = new Label();
            builtinBadge.textProperty().bind(i18n.createStringBinding("plugin.badge.builtin"));
            builtinBadge.getStyleClass().add("badge-builtin");
            buttons.getChildren().add(builtinBadge);
        }
        card.getChildren().addAll(header, authorLabel, descLabel, statusBadge, buttons);
        return card;
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
            setStatus("❌ Erreur : " + e.getMessage());
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
                setStatus("✅ " + i18n.get("plugin.status.uninstalled.success"));
            } catch (Exception e) {
                com.app.infrastructure.util.DailyLogger.logError("Plugins", "Failed to uninstall plugin " + plugin.id(), e);
                setStatus("❌ " + e.getMessage());
            }
        }
    }
    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
