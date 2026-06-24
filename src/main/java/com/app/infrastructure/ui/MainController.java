package com.app.infrastructure.ui;

import com.app.infrastructure.di.ServiceContext;
import com.app.infrastructure.sync.IncidentSyncManager;
import com.app.infrastructure.util.ConnectivityUtil;
import com.app.infrastructure.util.DailyLogger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.concurrent.CompletableFuture;

public class MainController {
    private final com.app.infrastructure.i18n.I18nService i18n = com.app.infrastructure.i18n.I18nService.getInstance();

    @FXML private BorderPane contentArea;
    @FXML private Label statusLabel;
    @FXML private VBox sidebar;
    @FXML private StackPane sidebarContainer;
    @FXML private SplitPane mainSplitPane;
    @FXML private Label navHeaderLabel;
    @FXML private VBox pluginsNavContainer;

    @FXML private Button btnHome;
    @FXML private Button btnIncidents;
    @FXML private Button btnQuery;
    @FXML private Button btnPlugins;
    @FXML private Button btnTheme;
    @FXML private Button btnSettings;
    @FXML private Button btnContentSidebarToggle;

    private Parent homeView;
    private Parent settingsView;
    private Parent themeView;
    private Parent pluginView;
    private Parent incidentView;
    private Parent queryView;

    private ServiceContext serviceContext;
    private IncidentSyncManager incidentSyncManager;

    private double lastDividerPosition = 0.25;
    private boolean isSidebarCollapsed = false;
    private boolean backgroundSyncStarted = false;

    public VBox getSidebar() {
        return sidebar;
    }

    public void setServiceContext(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
        this.incidentSyncManager = new IncidentSyncManager(serviceContext.getIncidentService());
    }

    public void startBackgroundSync() {
        if (serviceContext == null || backgroundSyncStarted) {
            return;
        }
        backgroundSyncStarted = true;
        this.serviceContext.getBackgroundSyncManager().startAutomaticSyncOnStartup();
    }

    public void setViews(Parent homeView, Parent settingsView, Parent themeView, Parent pluginView, Parent queryView, Parent incidentView) {
        this.homeView = homeView;
        this.settingsView = settingsView;
        this.themeView = themeView;
        this.pluginView = pluginView;
        this.queryView = queryView;
        this.incidentView = incidentView;
        showHome();
    }

    @FXML
    public void initialize() {
        AppState.getInstance().onlineProperty().addListener((obs, oldVal, newVal) -> {
            updateOnlineStatus(newVal);
            if (newVal && !oldVal) {
                handleBackOnline();
            }
        });

        AppState.getInstance().syncingProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                statusLabel.textProperty().bind(i18n.createStringBinding("main.status.syncing"));
            } else {
                updateOnlineStatus(AppState.getInstance().isOnline());
            }
        });

        bindI18n();
        checkConnectivity();
    }

    private void handleBackOnline() {
        if (!AppState.getInstance().isAuthenticated()) {
            App.triggerLogin();
            AppState.getInstance().accessTokenProperty().addListener(new javafx.beans.value.ChangeListener<String>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    if (newValue != null && !newValue.isBlank()) {
                        AppState.getInstance().accessTokenProperty().removeListener(this);
                        runSynchronization();
                    }
                }
            });
        } else {
            runSynchronization();
        }
    }

    private void runSynchronization() {
        if (incidentSyncManager == null || AppState.getInstance().isSyncing() || !AppState.getInstance().isOnline()) {
            return;
        }
        AppState.getInstance().setSyncing(true);
        CompletableFuture.runAsync(() -> {
            try {
                // S'assure que le SyncManager est lancé s'il y a un retour réseau
                incidentSyncManager.syncWithBackend(conflict -> conflict.localIncident());
            } catch (Exception e) {
                DailyLogger.logError("MainController", "Sync failed", e);
            } finally {
                Platform.runLater(() -> AppState.getInstance().setSyncing(false));
            }
        });
    }

    private void bindI18n() {
        navHeaderLabel.textProperty().bind(i18n.createStringBinding("nav.header"));
        btnHome.textProperty().bind(i18n.createStringBinding("nav.home"));
        btnIncidents.textProperty().bind(i18n.createStringBinding("nav.incidents"));
        btnQuery.textProperty().bind(i18n.createStringBinding("nav.query"));
        btnPlugins.textProperty().bind(i18n.createStringBinding("nav.plugins"));
        btnTheme.textProperty().bind(i18n.createStringBinding("nav.themes"));
        btnSettings.textProperty().bind(i18n.createStringBinding("settings.title"));
    }

    private void checkConnectivity() {
        statusLabel.textProperty().bind(i18n.createStringBinding("main.status.checking"));
        new Thread(() -> {
            boolean isConnected = ConnectivityUtil.checkConnectivity();
            Platform.runLater(() -> {
                AppState.getInstance().setOnline(isConnected);
                updateOnlineStatus(isConnected);
            });
        }).start();
    }

    private void updateOnlineStatus(boolean isOnline) {
        if (!AppState.getInstance().isSyncing()) {
            statusLabel.textProperty().bind(i18n.createStringBinding(isOnline ? "settings.online.active" : "settings.online.inactive"));
        }
    }

    @FXML
    private void showHome() {
        setActiveButton(btnHome);
        if (homeView != null) contentArea.setCenter(homeView);
    }

    @FXML
    private void showIncidents() {
        setActiveButton(btnIncidents);
        if (incidentView != null) contentArea.setCenter(incidentView);
    }

    @FXML
    private void showSettings() {
        setActiveButton(btnSettings);
        if (settingsView != null) contentArea.setCenter(settingsView);
    }

    @FXML
    private void showThemes() {
        setActiveButton(btnTheme);
        if (themeView != null) contentArea.setCenter(themeView);
    }

    @FXML
    private void showQuery() {
        setActiveButton(btnQuery);
        if (queryView != null) {
            contentArea.setCenter(queryView);
        } else {
            Label placeholder = new Label();
            placeholder.textProperty().bind(i18n.createStringBinding("query.view.not_loaded"));
            placeholder.getStyleClass().add("placeholder-text");
            contentArea.setCenter(placeholder);
        }
    }

    private void setActiveButton(Button btn) {
    }

    @FXML
    private void showPlugins() {
        setActiveButton(btnPlugins);
        if (pluginView != null) contentArea.setCenter(pluginView);
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleToggleSidebar() {
        if (isSidebarCollapsed) {
            sidebarContainer.setMaxWidth(400);
            sidebarContainer.setMinWidth(0);
            mainSplitPane.setDividerPositions(lastDividerPosition);
            sidebar.setVisible(true);
            isSidebarCollapsed = false;
        } else {
            lastDividerPosition = mainSplitPane.getDividerPositions()[0];
            sidebarContainer.setMaxWidth(0);
            sidebarContainer.setMinWidth(0);
            sidebar.setVisible(false);
            isSidebarCollapsed = true;
        }
    }

    public void addPluginMenuItem(MenuItem item) {
    }

    public void addPluginPanel(String title, Parent panelContent) {
        Platform.runLater(() -> {
            for (Node node : pluginsNavContainer.getChildren()) {
                if (node instanceof Button && title.equals(((Button)node).getText())) {
                    return;
                }
            }
            Button btn = new Button(title);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("sidebar-button");
            btn.setOnAction(e -> {
                contentArea.setCenter(panelContent);
                setActiveButton(btn);
            });
            pluginsNavContainer.getChildren().add(btn);
        });
    }

    public void removePluginPanel(String title) {
        Platform.runLater(() -> {
            pluginsNavContainer.getChildren().removeIf(node ->
                    node instanceof Button && title.equals(((Button)node).getText())
            );
        });
    }
}