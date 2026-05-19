package com.app.infrastructure.ui;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import com.app.domain.service.AuthService;
import com.app.domain.service.PluginService;
import com.app.domain.service.ThemeService;
import com.app.domain.service.UpdateService;
import com.app.infrastructure.adapter.auth.AuthenticatedHttpClient;
import com.app.infrastructure.adapter.auth.TokenManager;
import com.app.infrastructure.adapter.theme.JavaFXThemeAdapter;
import com.app.infrastructure.util.ConnectivityUtil;
import com.app.infrastructure.util.DailyLogger;
import com.app.plugin.PluginContext;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class App extends Application {
    private static Scene scene;
    private static Stage mainStage;
    private static com.app.infrastructure.di.ServiceContext serviceContextInstance;

    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;
        DailyLogger.getInstance().logAppStart();

        try {
            com.app.infrastructure.di.ServiceContext serviceContext = new com.app.infrastructure.di.ServiceContext(false);
            serviceContextInstance = serviceContext;

            try {
                serviceContext.initializeDatabase();
            } catch (Exception e) {
                DailyLogger.logError("App", "Database initialization failed", e);
            }

            AuthService authService = serviceContext.getAuthService();
            UpdateService updateService = serviceContext.getUpdateService();
            PluginService pluginService = serviceContext.getPluginService();
            PluginContext pluginContext = serviceContext.getPluginContext();

            JavaFXThemeAdapter themeAdapter = new JavaFXThemeAdapter();
            ThemeService themeService = new ThemeService(themeAdapter);
            ResourceBundle bundle = com.app.infrastructure.i18n.I18nService.getInstance().getBundle();

            boolean isInitiallyOnline = ConnectivityUtil.checkConnectivity();
            AppState.getInstance().setOnline(isInitiallyOnline);
            if (isInitiallyOnline) {
                runAutomaticUpdate(updateService);
            }

            if (serviceContext.isAuthBypassEnabled()) {
                String bypassToken = serviceContext.getAuthBypassToken();
                AppState.getInstance().setAccessToken(bypassToken);
            } else if (!isInitiallyOnline) {
                DailyLogger.logWarn("App", "Starting in offline mode. Skipping login.");
            } else {
                boolean autoLoginSuccess = attemptAutoLogin();
                if (!autoLoginSuccess) {
                    if (!showLoginDialog(stage, authService, bundle)) {
                        Platform.exit();
                        return;
                    }
                }
            }

            FXMLLoader mainLoader = new FXMLLoader(App.class.getResource("/com/app/view/MainView.fxml"));
            mainLoader.setResources(bundle);
            Parent root = mainLoader.load();
            MainController mainController = mainLoader.getController();

            mainController.setServiceContext(serviceContext);

            pluginContext.subscribe("PLUGIN_MENU_ADDED", item ->
                    mainController.addPluginMenuItem((javafx.scene.control.MenuItem) item));
            pluginContext.subscribe("PLUGIN_PANEL_ADDED", data -> {
                if (data instanceof com.app.infrastructure.adapter.plugin.DefaultPluginContext.PanelRegistration pr) {
                    mainController.addPluginPanel(pr.title(), pr.panel());
                }
            });
            pluginContext.subscribe("PLUGIN_PANEL_REMOVED", title -> {
                if (title instanceof String panelTitle) {
                    mainController.removePluginPanel(panelTitle);
                }
            });

            FXMLLoader settingsLoader = new FXMLLoader(App.class.getResource("/com/app/view/SettingsView.fxml"));
            settingsLoader.setResources(bundle);
            settingsLoader.setControllerFactory(param -> new SettingsController(updateService, themeAdapter));
            Parent settingsView = settingsLoader.load();

            FXMLLoader themeLoader = new FXMLLoader(App.class.getResource("/com/app/view/ThemeView.fxml"));
            themeLoader.setResources(bundle);
            themeLoader.setControllerFactory(param -> new ThemeController(themeService));
            Parent themeView = themeLoader.load();

            FXMLLoader pluginLoader = new FXMLLoader(App.class.getResource("/com/app/view/PluginView.fxml"));
            pluginLoader.setResources(bundle);
            pluginLoader.setControllerFactory(param -> new PluginController(pluginService));
            Parent pluginView = pluginLoader.load();

            FXMLLoader queryLoader = new FXMLLoader(App.class.getResource("/com/app/view/QueryView.fxml"));
            queryLoader.setResources(bundle);
            Parent queryView = queryLoader.load();

            FXMLLoader homeLoader = new FXMLLoader(App.class.getResource("/com/app/view/HomeView.fxml"));
            homeLoader.setResources(bundle);
            homeLoader.setControllerFactory(param -> new HomeController());
            Parent homeView = homeLoader.load();

            FXMLLoader incidentLoader = new FXMLLoader(App.class.getResource("/com/app/view/IncidentView.fxml"));
            incidentLoader.setResources(bundle);
            incidentLoader.setControllerFactory(param -> new IncidentController(serviceContext.getIncidentService(), serviceContext.getUserService()));
            Parent incidentView = incidentLoader.load();

            mainController.setViews(homeView, settingsView, themeView, pluginView, queryView, incidentView);
            scene = new Scene(root, 1000, 700);
            com.app.infrastructure.util.KeyboardShortcutsHandler.attachTo(scene);
            themeAdapter.setScene(scene);

            String savedTheme = themeAdapter.loadPreference();
            themeAdapter.applyTheme(savedTheme != null ? savedTheme : "default-dark");

            stage.setTitle("Connected-Neighbours-Java-App v" + updateService.getCurrentVersion());
            stage.setScene(scene);
            setStageIcon(stage);
            stage.setOnCloseRequest(e -> {
                DailyLogger.getInstance().logAppShutdown();
                AppState.getInstance().shutdown();
            });
            stage.show();

        } catch (RuntimeException | IOException t) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter("startup_error.log")) {
                t.printStackTrace(pw);
            } catch (IOException ex) {
            }
            throw new RuntimeException("Startup failed", t);
        }
    }

    public static void main(String[] args) {
        launch();
    }

    private void runAutomaticUpdate(UpdateService updateService) {
        updateService.installLatestUpdateIfAvailable()
                .exceptionally(ex -> {
                    DailyLogger.logError("App", "Automatic update failed", getRootCause(ex));
                    return false;
                });
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private boolean attemptAutoLogin() {
        String[] tokens = TokenManager.loadTokens();
        if (tokens != null && tokens[1] != null && !tokens[1].isEmpty()) {
            AuthenticatedHttpClient client = new AuthenticatedHttpClient();
            String newAccessToken = client.doRefresh(tokens[1]);
            if (newAccessToken != null) {
                TokenManager.saveTokens(newAccessToken, tokens[1]);
                AppState.getInstance().setAccessToken(newAccessToken);
                return true;
            } else {
                TokenManager.clearTokens();
            }
        }
        return false;
    }

    public static boolean showLoginDialog(Stage owner, AuthService authService, ResourceBundle bundle) throws IOException {
        AtomicBoolean authenticated = new AtomicBoolean(false);

        Stage loginStage = new Stage();
        loginStage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            loginStage.initOwner(owner);
        }
        loginStage.setResizable(false);
        loginStage.setTitle("Connected-Neighbours-Java-App - Login");

        URL iconUrl = App.class.getResource("/icon.png");
        if (iconUrl != null) {
            loginStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        FXMLLoader loginLoader = new FXMLLoader(App.class.getResource("/com/app/view/LoginView.fxml"));
        loginLoader.setResources(bundle);
        loginLoader.setControllerFactory(param -> {
            if (param == LoginController.class) {
                return new LoginController(authService, accessToken -> {
                    AppState.getInstance().setAccessToken(accessToken);
                    authenticated.set(true);
                    loginStage.close();
                });
            }
            try {
                return param.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to create controller " + param.getName(), e);
            }
        });

        Parent loginRoot = loginLoader.load();
        Scene loginScene = new Scene(loginRoot, 560, 420);

        JavaFXThemeAdapter loginThemeAdapter = new JavaFXThemeAdapter();
        loginThemeAdapter.setScene(loginScene);
        String savedTheme = loginThemeAdapter.loadPreference();
        loginThemeAdapter.applyTheme(savedTheme != null ? savedTheme : "default-dark");

        loginStage.setScene(loginScene);
        loginStage.showAndWait();

        return authenticated.get();
    }

    public static void triggerLogin() {
        Platform.runLater(() -> {
            try {
                AuthService authService = serviceContextInstance.getAuthService();
                ResourceBundle bundle = com.app.infrastructure.i18n.I18nService.getInstance().getBundle();
                showLoginDialog(mainStage, authService, bundle);
            } catch (IOException e) {
                DailyLogger.logError("App", "Failed to open login dialog", e);
            }
        });
    }

    private void setStageIcon(Stage stage) {
        URL iconUrl = App.class.getResource("/icon.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
    }
}
