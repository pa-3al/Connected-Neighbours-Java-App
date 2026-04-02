package com.app.infrastructure.ui;

import com.app.infrastructure.util.ConnectivityUtil;
import com.app.infrastructure.util.DailyLogger;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController {
    private final com.app.infrastructure.i18n.I18nService i18n = com.app.infrastructure.i18n.I18nService.getInstance();
    @FXML private BorderPane contentArea;
    @FXML private ToggleButton onlineToggle;
    @FXML private Label statusLabel;
    @FXML private VBox sidebar;
    @FXML private StackPane sidebarContainer;
    @FXML private SplitPane mainSplitPane;
    @FXML private Label navHeaderLabel;

    @FXML private Button btnHome;
    @FXML private Button btnIncidents;
    @FXML private Button btnQuery;
    @FXML private Button btnPlugins;
    @FXML private Button btnTheme;
    @FXML private Button btnSettings;
    @FXML private Button btnToggleSidebar;

    private Parent homeView;
    private Parent settingsView;
    private Parent themeView;
    private Parent pluginView;
    private Parent incidentView;
    private Parent queryView;

    private double lastDividerPosition = 0.25;
    private boolean isSidebarCollapsed = false;

    public VBox getSidebar() {
        return sidebar;
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
        });
        bindI18n();
        checkConnectivity();
        onlineToggle.setOnAction(e -> {
            if (onlineToggle.isSelected()) {
                checkConnectivity();
            } else {
                com.app.infrastructure.util.DailyLogger.logInfo("MainController", "User manually set status to OFFLINE");
                AppState.getInstance().setOnline(false);
            }
        });
    }

    private void bindI18n() {
        navHeaderLabel.textProperty().bind(i18n.createStringBinding("nav.header"));
        btnHome.textProperty().bind(i18n.createStringBinding("nav.home"));
        btnQuery.textProperty().bind(i18n.createStringBinding("nav.query"));
        btnPlugins.textProperty().bind(i18n.createStringBinding("nav.plugins"));
        btnTheme.textProperty().bind(i18n.createStringBinding("nav.themes"));
        btnSettings.textProperty().bind(i18n.createStringBinding("settings.title"));
    }

    private void checkConnectivity() {
        statusLabel.textProperty().bind(i18n.createStringBinding("main.status.checking"));
        DailyLogger.logDebug("MainController", "Initiating manual connectivity check...");
        new Thread(() -> {
            boolean isConnected = ConnectivityUtil.checkConnectivity();
            javafx.application.Platform.runLater(() -> {
                DailyLogger.logInfo("MainController", "Connectivity check result: " + (isConnected ? "ONLINE" : "OFFLINE"));
                AppState.getInstance().setOnline(isConnected);
                updateOnlineStatus(isConnected);
            });
        }).start();
    }

    private void updateOnlineStatus(boolean isOnline) {
        statusLabel.textProperty().bind(i18n.createStringBinding(isOnline ? "settings.online.active" : "settings.online.inactive"));

        if (onlineToggle.isSelected() != isOnline) {
             onlineToggle.setSelected(isOnline);
        }

        onlineToggle.setText(i18n.get(isOnline ? "settings.online.active" : "settings.online.inactive"));
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
        if (pluginView != null) contentArea.setCenter(pluginView);
    }

    @FXML
    private void handleExit() {
        javafx.application.Platform.exit();
    }

    @FXML private Button btnContentSidebarToggle;

    @FXML
    private void handleToggleSidebar() {
        if (isSidebarCollapsed) {

            sidebarContainer.setMaxWidth(400); 
            sidebarContainer.setMinWidth(0);
            mainSplitPane.setDividerPositions(lastDividerPosition);
            if (btnToggleSidebar != null) btnToggleSidebar.setText("◀");
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
         DailyLogger.logWarn("MainController", "addPluginMenuItem called but Menus are disabled.");
    }

    public void addPluginPanel(String title, Parent panelContent) {
        javafx.application.Platform.runLater(() -> {

            for (Node node : sidebar.getChildren()) {
                if (node instanceof javafx.scene.control.Button && title.equals(((javafx.scene.control.Button)node).getText())) {
                    return;
                }
            }

            javafx.scene.control.Button btn = new javafx.scene.control.Button(title);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("nav-button");
            btn.setOnAction(e -> {
                 contentArea.setCenter(panelContent);
                 setActiveButton(btn);
            });

            int insertIndex = -1;
            for (int i = 0; i < sidebar.getChildren().size(); i++) {
                Node n = sidebar.getChildren().get(i);
                if (n == btnTheme) {
                    insertIndex = i + 1;
                    break;
                }
            }

            if (insertIndex == -1) {
                for (int i = 0; i < sidebar.getChildren().size(); i++) {
                    Node n = sidebar.getChildren().get(i);
                    if (n instanceof javafx.scene.layout.Region && !(n instanceof javafx.scene.control.Control)) {
                        insertIndex = i;
                        break;
                    }
                }
            }

            if (insertIndex >= 0 && insertIndex < sidebar.getChildren().size()) {
                sidebar.getChildren().add(insertIndex, btn);
            } else {
                sidebar.getChildren().add(btn);
            }
        });
    }

    public void removePluginPanel(String title) {
        javafx.application.Platform.runLater(() -> {
            sidebar.getChildren().removeIf(node -> 
                node instanceof javafx.scene.control.Button && title.equals(((javafx.scene.control.Button)node).getText())
            );
        });
    }
}
