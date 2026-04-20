package com.app.infrastructure.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.app.domain.model.Theme;
import com.app.domain.port.in.ThemeUseCase;
import com.app.domain.service.ThemeService;
import com.app.infrastructure.ui.theme.ThemeCardFactory;
import com.app.infrastructure.ui.theme.ThemeColorFactory;
import com.app.infrastructure.ui.theme.ThemeColors;
import com.app.infrastructure.ui.theme.ThemeCssGenerator;
import com.app.infrastructure.ui.theme.ThemeEditorDialog;
import com.app.infrastructure.util.KeyboardShortcutsHandler;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

public class ThemeController {

    private final com.app.infrastructure.i18n.I18nService i18n = com.app.infrastructure.i18n.I18nService.getInstance();
    private final ThemeColorFactory colorFactory = new ThemeColorFactory();
    private final ThemeCssGenerator cssGenerator = new ThemeCssGenerator();
    private final ThemeEditorDialog themeEditorDialog = new ThemeEditorDialog(i18n);
    private final ThemeCardFactory themeCardFactory = new ThemeCardFactory(i18n);

    private final ThemeUseCase themeUseCase;

    @FXML
    private ScrollPane themeRoot;
    @FXML
    private FlowPane themesContainer;
    @FXML
    private Label currentThemeLabel;
    @FXML
    private TextField themeUrlField;
    @FXML
    private Button downloadButton;
    @FXML
    private Label statusLabel;

    public ThemeController(ThemeUseCase themeUseCase) {
        this.themeUseCase = themeUseCase;
    }

    @FXML
    public void initialize() {
        refreshThemesList();
        updateCurrentThemeLabel();
        KeyboardShortcutsHandler.registerContext(themeRoot, this::handleShortcut);
    }

    private boolean handleShortcut(KeyboardShortcutsHandler.ShortcutAction action, Node focusOwner) {
        if (focusOwner == null) {
            return false;
        }
        return switch (action) {
            case NEW_ITEM -> {
                handleCreateTheme();
                yield true;
            }
            case REFRESH -> {
                refreshThemesList();
                updateCurrentThemeLabel();
                yield true;
            }
            default -> false;
        };
    }

    private void refreshThemesList() {
        themesContainer.getChildren().clear();
        if (themeUseCase instanceof ThemeService themeService) {
            themeService.reloadThemes();
        }

        List<Theme> themes = themeUseCase.getAvailableThemes();
        Theme currentTheme = themeUseCase.getCurrentTheme();

        for (Theme theme : themes) {
            themesContainer.getChildren().add(
                    themeCardFactory.create(
                            theme,
                            currentTheme,
                            this::handleApplyTheme,
                            this::handleEditTheme,
                            this::handleExportTheme,
                            this::handleDeleteTheme
                    )
            );
        }
    }

    private void handleApplyTheme(String themeId) {
        try {
            com.app.infrastructure.util.DailyLogger.logInfo("Themes", "User applying theme: " + themeId);
            themeUseCase.switchTheme(themeId);
            updateCurrentThemeLabel();
            refreshThemesList();
            setStatus("✅ " + i18n.get("theme.status.applied"));
        } catch (Exception e) {
            com.app.infrastructure.util.DailyLogger.logError("Themes", "Failed to apply theme " + themeId, e);
            setStatus("❌ Erreur : " + e.getMessage());
        }
    }

    private void handleDeleteTheme(Theme theme) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.titleProperty().bind(i18n.createStringBinding("theme.delete.title"));
        confirm.headerTextProperty().bind(i18n.createStringBinding("theme.delete.header", theme.name()));
        confirm.contentTextProperty().bind(i18n.createStringBinding("theme.delete.content"));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                com.app.infrastructure.util.DailyLogger.logWarn(
                        "Themes",
                        "User deleting theme: " + theme.name() + " (" + theme.id() + ")"
                );
                themeUseCase.deleteTheme(theme.id());
                refreshThemesList();
                setStatus("✅ Thème supprimé");
            } catch (Exception e) {
                com.app.infrastructure.util.DailyLogger.logError("Themes", "Failed to delete theme " + theme.id(), e);
                setStatus("❌ " + e.getMessage());
            }
        }
    }

    private void handleExportTheme(Theme theme) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(i18n.get("theme.export.title"));
        fileChooser.setInitialFileName(theme.id() + ".zip");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichier ZIP (*.zip)", "*.zip")
        );

        java.io.File file = fileChooser.showSaveDialog(themesContainer.getScene().getWindow());
        if (file != null) {
            try {
                com.app.infrastructure.util.DailyLogger.logInfo(
                        "Themes",
                        "User exporting theme " + theme.id() + " to " + file.getPath()
                );
                themeUseCase.exportTheme(theme.id(), file.toPath());
                setStatus("✅ " + i18n.get("theme.export.success", file.getName()));
            } catch (Exception e) {
                com.app.infrastructure.util.DailyLogger.logError("Themes", "Failed to export theme " + theme.id(), e);
                setStatus("❌ " + i18n.get("theme.export.error", e.getMessage()));
            }
        }
    }

    private void handleEditTheme(Theme theme) {
        Map<String, String> props = themeUseCase.getThemeProperties(theme.id());
        if (props == null || props.isEmpty()) {
            setStatus("❌ " + i18n.get("theme.edit.error.nodata"));
            return;
        }

        ThemeColors colors = colorFactory.readThemeColors(props, colorFactory.defaultForEdit());
        openThemeEditor(theme.name(), theme.author(), colors, true);
    }

    @FXML
    private void handleImportThemeLocal() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(i18n.get("theme.import.title"));
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichier ZIP (*.zip)", "*.zip")
        );

        java.io.File file = fileChooser.showOpenDialog(themesContainer.getScene().getWindow());
        if (file != null) {
            try {
                com.app.infrastructure.util.DailyLogger.logInfo("Themes", "User importing theme from " + file.getPath());
                themeUseCase.importTheme(file.toPath());
                refreshThemesList();
                setStatus("✅ " + i18n.get("theme.import.success", file.getName()));
            } catch (Exception e) {
                com.app.infrastructure.util.DailyLogger.logError("Themes", "Failed to import theme", e);
                setStatus("❌ " + i18n.get("theme.import.error", e.getMessage()));
            }
        }
    }

    @FXML
    private void handleDownloadTheme() {
        String url = themeUrlField.getText();
        if (url == null || url.isBlank()) {
            setStatus("❌ " + i18n.get("theme.download.error.url"));
            return;
        }

        setStatus(i18n.get("theme.download.progress"));
        downloadButton.setDisable(true);
        com.app.infrastructure.util.DailyLogger.logInfo("Themes", "User started theme download from URL: " + url);

        themeUseCase.downloadTheme(url)
                .thenAccept(theme -> Platform.runLater(() -> {
                    com.app.infrastructure.util.DailyLogger.logInfo("Themes", "Theme downloaded successfully: " + theme.id());
                    downloadButton.setDisable(false);
                    themeUrlField.clear();
                    refreshThemesList();
                    setStatus("✅ " + i18n.get("theme.download.success", theme.name()));
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        com.app.infrastructure.util.DailyLogger.logError("Themes", "Theme download failed", ex);
                        downloadButton.setDisable(false);
                        setStatus("❌ " + i18n.get("theme.download.error", getRootCause(ex).getMessage()));
                    });
                    return null;
                });
    }

    @FXML
    private void handleCreateTheme() {
        ThemeColors initialColors = colorFactory.defaultForCreate();
        Theme currentTheme = themeUseCase.getCurrentTheme();

        if (currentTheme != null) {
            Map<String, String> props = themeUseCase.getThemeProperties(currentTheme.id());
            if (props != null && !props.isEmpty()) {
                initialColors = colorFactory.readThemeColors(props, initialColors);
            }
        }

        openThemeEditor("", "", initialColors, false);
    }

    private void openThemeEditor(String initialName, String initialAuthor, ThemeColors initialColors, boolean isEdit) {
        Optional<ThemeEditorDialog.ThemeDraft> draft = themeEditorDialog.show(
                themesContainer.getScene() == null ? null : themesContainer.getScene().getWindow(),
                initialName,
                initialAuthor,
                initialColors,
                isEdit
        );

        if (draft.isEmpty()) {
            return;
        }

        try {
            String newThemeId = createCustomTheme(draft.get().name(), draft.get().author(), draft.get().colors());
            refreshThemesList();

            Theme current = themeUseCase.getCurrentTheme();
            if (current != null && current.id().equals(newThemeId)) {
                handleApplyTheme(newThemeId);
            }

            String action = isEdit ? i18n.get("theme.action.modified") : i18n.get("theme.action.created");
            setStatus("✅ " + i18n.get("theme.create.success", draft.get().name(), action));
        } catch (IOException e) {
            setStatus("❌ Erreur : " + e.getMessage());
            com.app.infrastructure.util.DailyLogger.logError("Themes", "Failed to create theme", e);
        }
    }

    private String createCustomTheme(String name, String author, ThemeColors colors) throws IOException {
        String themeId = "custom-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        com.app.infrastructure.util.DailyLogger.logInfo("Themes", "User creating custom theme: " + themeId + " (" + name + ")");

        Path themeDir = Paths.get("themes", "custom", themeId);
        Files.createDirectories(themeDir);

        String css = cssGenerator.generate(colors);
        Files.writeString(themeDir.resolve("theme.css"), css);

        String colorsJson = colors.toMap().entrySet().stream()
                .map(entry -> String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(",\n    "));

        String manifest = String.format(
                """
                    {
                      "id": "%s",
                      "name": "%s",
                      "author": "%s",
                      "description": "Thème personnalisé créé avec l'éditeur avancé",
                      "cssFile": "theme.css",
                      "colors": {
                        %s
                      }
                    }
                    """,
                themeId,
                name,
                author,
                colorsJson
        );

        Files.writeString(themeDir.resolve("theme.json"), manifest);
        com.app.infrastructure.util.DailyLogger.logInfo("Themes", "Custom theme created at: " + themeDir.toAbsolutePath());
        return themeId;
    }

    private void updateCurrentThemeLabel() {
        Theme current = themeUseCase.getCurrentTheme();
        currentThemeLabel.setText("Thème actuel : " + (current != null ? current.name() : "Aucun"));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
