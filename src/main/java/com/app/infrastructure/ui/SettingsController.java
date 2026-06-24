package com.app.infrastructure.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import com.app.domain.port.in.CheckUpdateUseCase;
import com.app.domain.port.out.ThemeRepository;
import com.app.infrastructure.util.KeyboardShortcutsHandler;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;

public class SettingsController {
    private final CheckUpdateUseCase checkUpdateUseCase;
    private final com.app.infrastructure.config.ConfigProvider configProvider = new com.app.infrastructure.config.ConfigProvider();

    @FXML private ScrollPane settingsRoot;
    @FXML private Label settingsTitleLabel;
    @FXML private Label generalLabel;
    @FXML private Label languageLabel;
    @FXML private Label updatesTitleLabel;
    @FXML private Label versionLabel;
    @FXML private Label statusLabel;
    @FXML private Label progressLabel;
    @FXML private Label updateLogTitleLabel;

    @FXML private TextArea updateLogArea;
    @FXML private ProgressBar downloadProgressBar;
    @FXML private Button checkButton;
    @FXML private Button downloadButton;
    @FXML private Button installButton;
    @FXML private HBox progressContainer;
    @FXML private ToggleButton onlineToggle;

    @FXML private Label uninstallTitleLabel;
    @FXML private Label uninstallDescLabel;
    @FXML private Button uninstallButton;

    @FXML private ComboBox<java.util.Locale> languageCombo;

    @FXML private Label dbConfigTitleLabel;
    @FXML private Label dbUrlLabel;
    @FXML private Label dbUserLabel;
    @FXML private Label dbPasswordLabel;
    @FXML private TextField dbUrlField;
    @FXML private TextField dbUserField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Button saveDbButton;
    @FXML private Button testDbButton;

    private com.app.domain.model.UpdateInfo pendingUpdate;
    private Path downloadedUpdate;
    private boolean toggleGuard = false;
    private final com.app.infrastructure.i18n.I18nService i18n = com.app.infrastructure.i18n.I18nService.getInstance();

    public SettingsController(CheckUpdateUseCase checkUpdateUseCase, ThemeRepository themeRepository) {
        this.checkUpdateUseCase = checkUpdateUseCase;
    }

    @FXML
    public void initialize() {
        bindI18n();
        setupLanguageCombo();
        setupOnlineToggle();

        versionLabel.textProperty().bind(
                i18n.createStringBinding("settings.updates.version.prefix")
                        .concat(checkUpdateUseCase.getCurrentVersion())
        );
        resetDownloadUI();
        loadDbConfig();

        KeyboardShortcutsHandler.registerContext(settingsRoot, this::handleShortcut);
    }

    private boolean handleShortcut(KeyboardShortcutsHandler.ShortcutAction action, Node focusOwner) {
        if (action != KeyboardShortcutsHandler.ShortcutAction.SAVE) {
            return false;
        }
        if (!isDbFieldFocused(focusOwner)) {
            return false;
        }
        handleSaveDbConfig();
        return true;
    }

    private boolean isDbFieldFocused(Node focusOwner) {
        return focusOwner == dbUrlField || focusOwner == dbUserField || focusOwner == dbPasswordField;
    }

    private void bindI18n() {
        settingsTitleLabel.textProperty().bind(i18n.createStringBinding("settings.title"));
        generalLabel.textProperty().bind(i18n.createStringBinding("settings.general"));
        languageLabel.textProperty().bind(i18n.createStringBinding("settings.language"));
        updatesTitleLabel.textProperty().bind(i18n.createStringBinding("settings.updates.title"));
        progressLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.downloading", 0));
        updateLogTitleLabel.textProperty().bind(i18n.createStringBinding("settings.updates.log.title"));
        updateLogArea.promptTextProperty().bind(i18n.createStringBinding("settings.updates.log.placeholder"));

        checkButton.textProperty().bind(i18n.createStringBinding("settings.updates.check.button"));
        downloadButton.textProperty().bind(i18n.createStringBinding("settings.updates.download.button"));
        installButton.textProperty().bind(i18n.createStringBinding("settings.updates.install.button"));

        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.uptodate"));
        if (testDbButton != null) testDbButton.textProperty().bind(i18n.createStringBinding("settings.db.test"));
        if (uninstallTitleLabel != null) {
            uninstallTitleLabel.textProperty().bind(i18n.createStringBinding("settings.uninstall.title"));
        }
        if (uninstallDescLabel != null) {
            uninstallDescLabel.textProperty().bind(i18n.createStringBinding("settings.uninstall.description"));
        }
        if (uninstallButton != null) {
            uninstallButton.textProperty().bind(i18n.createStringBinding("settings.uninstall.button"));
        }

        if (dbConfigTitleLabel != null) dbConfigTitleLabel.textProperty().bind(i18n.createStringBinding("settings.db.title"));
        if (dbUrlLabel != null) dbUrlLabel.textProperty().bind(i18n.createStringBinding("settings.db.url"));
        if (dbUserLabel != null) dbUserLabel.textProperty().bind(i18n.createStringBinding("settings.db.user"));
        if (dbPasswordLabel != null) dbPasswordLabel.textProperty().bind(i18n.createStringBinding("settings.db.password"));
        if (saveDbButton != null) saveDbButton.textProperty().bind(i18n.createStringBinding("settings.db.save"));
    }

    private void setupLanguageCombo() {
        languageCombo.getItems().setAll(i18n.getAvailableLocales());
        languageCombo.setValue(i18n.localeProperty().get());
        languageCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(java.util.Locale object) {
                if (object == null) return "";
                if (object.equals(java.util.Locale.FRENCH)) return com.vdurmont.emoji.EmojiParser.parseToUnicode(i18n.get("language.french"));
                if (object.equals(java.util.Locale.ENGLISH)) return com.vdurmont.emoji.EmojiParser.parseToUnicode(i18n.get("language.english"));

                String display = object.getDisplayName();
                if (display == null || display.isBlank() || display.equalsIgnoreCase(object.getLanguage())) {
                    return object.getLanguage() + " (" + i18n.get("language.user") + ")";
                }
                return display + " [" + object.getLanguage() + "]";
            }

            @Override
            public java.util.Locale fromString(String string) {
                return null;
            }
        });
        languageCombo.valueProperty().addListener((obs, old, neu) -> {
            if (neu != null) {
                com.app.infrastructure.util.DailyLogger.logDebug("Settings", "Selected locale: " + neu);
                i18n.setLocale(neu);
            }
        });
    }

    private void setupOnlineToggle() {
        AppState appState = AppState.getInstance();
        onlineToggle.setSelected(appState.isOnline());
        onlineToggle.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> appState.isOnline()
                                ? i18n.get("settings.online.active")
                                : i18n.get("settings.online.inactive"),
                        appState.onlineProperty(), i18n.localeProperty()
                )
        );
        appState.onlineProperty().addListener((obs, oldVal, newVal) -> {
            if (onlineToggle.isSelected() != newVal) {
                toggleGuard = true;
                onlineToggle.setSelected(newVal);
                toggleGuard = false;
            }
        });
        onlineToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (toggleGuard) return;
            if (newVal) {
                appState.forceOnline();
            } else {
                appState.forceOffline();
            }
            log(i18n.get(newVal ? "settings.online.log.active" : "settings.online.log.inactive"));
        });
        if (testDbButton != null) {
            testDbButton.disableProperty().bind(appState.onlineProperty().not());
        }
    }

    private void resetDownloadUI() {
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);
        downloadButton.setDisable(true);
        downloadButton.setVisible(false);
        installButton.setDisable(true);
        installButton.setVisible(false);
        downloadProgressBar.setProgress(0);
    }

    private void loadDbConfig() {
        String syncUrl = configProvider.getSyncDatabaseUrl();
        String user = "";
        String pass = "";
        String hostDb = syncUrl;

        if (syncUrl != null && syncUrl.startsWith("postgresql://")) {
            String withoutScheme = syncUrl.substring(13);
            int atIndex = withoutScheme.indexOf('@');

            if (atIndex != -1) {
                String credentials = withoutScheme.substring(0, atIndex);
                hostDb = withoutScheme.substring(atIndex + 1);

                int colonIndex = credentials.indexOf(':');
                if (colonIndex != -1) {
                    user = credentials.substring(0, colonIndex);
                    pass = credentials.substring(colonIndex + 1);
                } else {
                    user = credentials;
                }
            } else {
                hostDb = withoutScheme;
            }
        }

        if (dbUrlField != null) dbUrlField.setText(hostDb);
        if (dbUserField != null) dbUserField.setText(user);
        if (dbPasswordField != null) dbPasswordField.setText(pass);
    }

    @FXML
    private void handleSaveDbConfig() {
        String formattedUrl = getFormattedDatabaseString();

        configProvider.saveSyncDatabaseConfig(formattedUrl);
        log(i18n.get("settings.db.save.success"));
    }

    @FXML
    private void handleCheckUpdates() {
        log(i18n.get("settings.updates.status.checking"));
        com.app.infrastructure.util.DailyLogger.logInfo("Settings", "User initiated update check");
        checkButton.setDisable(true);
        statusLabel.textProperty().unbind();
        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.checking"));

        checkUpdateUseCase.checkForUpdates()
                .thenAccept(info -> Platform.runLater(() -> {
                    checkButton.setDisable(false);
                    if (info != null) {
                        pendingUpdate = info;
                        downloadedUpdate = null;
                        statusLabel.textProperty().unbind();
                        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.found", info.version()));
                        log(i18n.get("settings.updates.status.found", info.version()));
                        log(i18n.get("settings.updates.description", info.description()));
                        com.app.infrastructure.util.DailyLogger.logInfo("Settings", "Update found: " + info.version());
                        if (info.changelog() != null && !info.changelog().isEmpty()) {
                            log(i18n.get("settings.updates.changelog", info.changelog()));
                        }
                        downloadButton.setVisible(true);
                        downloadButton.setDisable(false);
                    } else {
                        statusLabel.textProperty().unbind();
                        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.uptodate"));
                        log(i18n.get("settings.updates.status.uptodate"));
                        pendingUpdate = null;
                        downloadedUpdate = null;
                        resetDownloadUI();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        checkButton.setDisable(false);
                        statusLabel.textProperty().unbind();
                        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.error"));
                        log(i18n.get("status.error", i18n.get("settings.updates.error.detail", getRootCause(ex).getMessage())));
                    });
                    return null;
                });
    }

    @FXML
    private void handleDownloadUpdate() {
        if (pendingUpdate == null) return;
        log(i18n.get("settings.updates.status.downloading", 0));
        com.app.infrastructure.util.DailyLogger.logInfo("Settings", "User started download for update: " + pendingUpdate.version());
        downloadButton.setDisable(true);
        progressContainer.setVisible(true);
        progressContainer.setManaged(true);
        downloadProgressBar.setProgress(0);
        statusLabel.textProperty().unbind();
        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.downloading", 0));

        checkUpdateUseCase.downloadUpdate(pendingUpdate, progress -> {
                    Platform.runLater(() -> {
                        downloadProgressBar.setProgress(progress.percentage() / 100.0);
                        String pct = String.format("%.1f", progress.percentage());
                        statusLabel.textProperty().unbind();
                        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.downloading", pct));
                    });
                })
                .thenAccept(path -> Platform.runLater(() -> {
                    downloadedUpdate = path;
                    log(i18n.get("status.success", i18n.get("settings.updates.status.ready")));
                    statusLabel.textProperty().unbind();
                    statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.ready"));
                    installButton.setVisible(true);
                    installButton.setDisable(false);
                    downloadButton.setVisible(false);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        downloadButton.setDisable(false);
                        statusLabel.textProperty().unbind();
                        statusLabel.textProperty().bind(i18n.createStringBinding("settings.updates.status.download.failed"));
                        log(i18n.get("status.error", i18n.get("settings.updates.download.error.detail", getRootCause(ex).getMessage())));
                        progressContainer.setVisible(false);
                        progressContainer.setManaged(false);
                    });
                    return null;
                });
    }

    @FXML
    private void handleInstallUpdate() {
        log(i18n.get("settings.updates.install.button"));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(i18n.get("dialog.update.title"));
        alert.setHeaderText(i18n.get("dialog.update.header"));
        alert.setContentText(i18n.get("dialog.update.content"));
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                log(i18n.get("settings.updates.restart"));
                com.app.infrastructure.util.DailyLogger.logWarn("Settings", "User confirmed update installation. Application restarting.");
                if (downloadedUpdate != null) {
                    checkUpdateUseCase.applyUpdate(downloadedUpdate);
                }
            } else {
                com.app.infrastructure.util.DailyLogger.logInfo("Settings", "User cancelled update installation");
            }
        });
    }

    @FXML
    private void handleUninstall() {
        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle(i18n.get("settings.uninstall.dialog.title"));
        confirmDialog.setHeaderText(i18n.get("settings.uninstall.dialog.header"));
        confirmDialog.setContentText(i18n.get("settings.uninstall.dialog.content"));

        ButtonType confirmButton = new ButtonType(i18n.get("settings.uninstall.dialog.confirm"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(i18n.get("settings.uninstall.dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(confirmButton, cancelButton);

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == confirmButton) {
                com.app.infrastructure.util.DailyLogger.logWarn("Settings", "User initiated application uninstall");
                performUninstall();
            }
        });
    }

    private void performUninstall() {
        String[] dirsToDelete = {"data", "config", "logs", "updates", "plugins", "themes"};
        StringBuilder report = new StringBuilder();

        for (String dir : dirsToDelete) {
            Path dirPath = Paths.get(dir);
            if (Files.exists(dirPath)) {
                try {
                    Files.walk(dirPath)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException e) {
                                    report.append(i18n.get("settings.uninstall.error.file", p.toString())).append("\n");
                                }
                            });
                    report.append(i18n.get("status.success", dir)).append("\n");
                } catch (IOException e) {
                    report.append(i18n.get("status.error", i18n.get("settings.uninstall.item.error", dir, e.getMessage()))).append("\n");
                }
            }
        }

        Path startupLog = Paths.get("startup_error.log");
        try {
            Files.deleteIfExists(startupLog);
        } catch (IOException ignored) {}

        try {
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

            File jarFile = new File(jarPath);
            if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                triggerSelfDestruct(jarFile);
                report.append(i18n.get("status.success", i18n.get("settings.uninstall.selfdestruct.scheduled"))).append("\n");
            }
        } catch (Exception e) {
            report.append(i18n.get("status.error", i18n.get("settings.uninstall.selfdestruct.failed", e.getMessage()))).append("\n");
            com.app.infrastructure.util.DailyLogger.logError("Uninstall", "Failed to schedule self-destruct", e);
        }

        Alert doneDialog = new Alert(Alert.AlertType.INFORMATION);
        doneDialog.setTitle(i18n.get("settings.uninstall.done.title"));
        doneDialog.setHeaderText(i18n.get("settings.uninstall.done.header"));
        doneDialog.setContentText(i18n.get("settings.uninstall.done.content") + "\n\n" + report);
        doneDialog.showAndWait();

        com.app.infrastructure.util.DailyLogger.logWarn("Settings", "Uninstall complete. Shutting down.");
        AppState.getInstance().shutdown();
        Platform.exit();
        System.exit(0);
    }

    private void triggerSelfDestruct(File jarFile) {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb = null;

        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", "ping -n 3 127.0.0.1 > NUL & del /f /q \"" + jarFile.getAbsolutePath() + "\"");
        } else {
            pb = new ProcessBuilder("sh", "-c", "sleep 3; rm -f \"" + jarFile.getAbsolutePath() + "\"");
        }

        if (pb != null) {
            try {
                pb.start();
                com.app.infrastructure.util.DailyLogger.logInfo("Uninstall", "Self-destruct process started for: " + jarFile.getAbsolutePath());
            } catch (IOException e) {
                com.app.infrastructure.util.DailyLogger.logError("Uninstall", "Failed to start self-destruct process", e);
            }
        }
    }

    private void log(String message) {
        updateLogArea.appendText(message + "\n");
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String getFormattedDatabaseString(){
        String hostDb = dbUrlField.getText() != null ? dbUrlField.getText() : "";
        String user = dbUserField.getText() != null ? dbUserField.getText() : "";
        String pass = dbPasswordField.getText() != null ? dbPasswordField.getText() : "";

        return String.format("postgresql://%s:%s@%s", user, pass, hostDb);
    }

    @FXML
    private void handleTestDbConfig() {
        if (!AppState.getInstance().isOnline()) {
            log(i18n.get("status.error", i18n.get("sync.offline.blocked")));
            return;
        }
        String hostDb = dbUrlField.getText() != null ? dbUrlField.getText() : "";
        String user = dbUserField.getText() != null ? dbUserField.getText() : "";
        String pass = dbPasswordField.getText() != null ? dbPasswordField.getText() : "";

        String jdbcUrl = "jdbc:postgresql://" + hostDb;

        log(i18n.get("settings.db.test.checking"));

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, user, pass)) {
            if (conn != null && !conn.isClosed()) {
                log(i18n.get("settings.db.test.success"));
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(i18n.get("dialog.db.test.info.title"));
                alert.setHeaderText(null);
                alert.setContentText(i18n.get("settings.db.test.success"));
                alert.showAndWait();
            }
        } catch (java.sql.SQLException e) {
            log(i18n.get("status.error", i18n.get("settings.db.test.fail", e.getMessage())));
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(i18n.get("status.error"));
            alert.setHeaderText(i18n.get("settings.db.test.error.header"));
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
