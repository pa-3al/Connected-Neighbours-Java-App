package com.app.infrastructure.ui;
import com.app.domain.service.PluginService;
import com.app.domain.service.ThemeService;
import com.app.domain.service.UpdateService;
import com.app.plugin.PluginContext;
import com.app.infrastructure.adapter.theme.JavaFXThemeAdapter;
import com.app.infrastructure.util.DailyLogger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
public class App extends Application {
    private static Scene scene;
    @Override
    public void start(Stage stage) throws IOException {
        DailyLogger.getInstance().logAppStart();
        DailyLogger.logInfo("App", "Initializing JavaFX application...");
        try {
            DailyLogger.logDebug("App", "Creating ServiceContext...");
            com.app.infrastructure.di.ServiceContext serviceContext = new com.app.infrastructure.di.ServiceContext(false);
            try {
                serviceContext.initializeDatabase();
                DailyLogger.logInfo("App", "Database initialized.");
            } catch (Exception e) {
                DailyLogger.logError("App", "Database initialization failed", e);
            } 
            UpdateService updateService = serviceContext.getUpdateService();
            PluginService pluginService = serviceContext.getPluginService();
            DailyLogger.logInfo("App", "Services initialized successfully");

            PluginContext pluginContext = serviceContext.getPluginContext();

            JavaFXThemeAdapter themeAdapter = new JavaFXThemeAdapter();
            ThemeService themeService = new ThemeService(themeAdapter);
            DailyLogger.logDebug("App", "Theme system initialized");
            DailyLogger.logDebug("App", "Loading FXML views...");

            java.util.ResourceBundle bundle = com.app.infrastructure.i18n.I18nService.getInstance().getBundle();

            FXMLLoader mainLoader = new FXMLLoader(App.class.getResource("/com/app/view/MainView.fxml"));
            mainLoader.setResources(bundle);
            Parent root = mainLoader.load();
            MainController mainController = mainLoader.getController();

            pluginContext.subscribe("PLUGIN_MENU_ADDED", item -> 
                mainController.addPluginMenuItem((javafx.scene.control.MenuItem) item));
            pluginContext.subscribe("PLUGIN_PANEL_ADDED", data -> {
                if (data instanceof com.app.infrastructure.adapter.plugin.DefaultPluginContext.PanelRegistration pr) {
                     mainController.addPluginPanel(pr.title(), pr.panel());
                }
            });
            pluginContext.subscribe("PLUGIN_PANEL_REMOVED", title -> {
                if (title instanceof String) {
                    mainController.removePluginPanel((String) title);
                }
            });

            com.app.plugin.impl.DrawingPlugin drawingPlugin = new com.app.plugin.impl.DrawingPlugin();
            drawingPlugin.onLoad(pluginContext);
            com.app.plugin.i18n.TranslationPlugin translationPlugin = new com.app.plugin.i18n.TranslationPlugin();
            translationPlugin.onLoad(pluginContext);

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
            incidentLoader.setControllerFactory(param -> new IncidentController(serviceContext.getIncidentService()));
            Parent incidentView = incidentLoader.load();

            DailyLogger.logInfo("App", "All views loaded successfully");
            mainController.setViews(homeView, settingsView, themeView, pluginView, queryView, incidentView);
            scene = new Scene(root, 1000, 700);
            com.app.infrastructure.util.KeyboardShortcutsHandler.attachTo(scene);
            themeAdapter.setScene(scene);
            String savedTheme = themeAdapter.loadPreference();
            themeAdapter.applyTheme(savedTheme != null ? savedTheme : "default-dark");
            DailyLogger.logInfo("App", "Theme applied: " + (savedTheme != null ? savedTheme : "default-dark"));
            stage.setTitle("PA");
            stage.setScene(scene);
            stage.setOnCloseRequest(e -> {
                DailyLogger.getInstance().logAppShutdown();
                AppState.getInstance().shutdown();
            });
            stage.show();
            DailyLogger.logInfo("App", "Application window displayed");
        } catch (Throwable t) {
            DailyLogger.logError("App", "FATAL: Application startup failed", t);
            try (java.io.PrintWriter pw = new java.io.PrintWriter("startup_error.log")) {
                t.printStackTrace(pw);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new RuntimeException("Startup failed", t);
        }
    }
    public static void main(String[] args) {
        launch();
    }
}
