package com.app.infrastructure.ui;
import com.app.domain.model.Theme;
import com.app.domain.port.in.ThemeUseCase;
import com.app.domain.service.ThemeService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
public class ThemeController {
    private final com.app.infrastructure.i18n.I18nService i18n = com.app.infrastructure.i18n.I18nService.getInstance();
    private final ThemeUseCase themeUseCase;
    @FXML private FlowPane themesContainer;
    @FXML private Label currentThemeLabel;
    @FXML private TextField themeUrlField;
    @FXML private Button downloadButton;

    @FXML private Label statusLabel;
    
    public ThemeController(ThemeUseCase themeUseCase) {
        this.themeUseCase = themeUseCase;
    }
    @FXML
    public void initialize() {
        refreshThemesList();
        updateCurrentThemeLabel();
    }
    private void refreshThemesList() {
        themesContainer.getChildren().clear();
        if (themeUseCase instanceof ThemeService themeService) {
            themeService.reloadThemes();
        }
        List<Theme> themes = themeUseCase.getAvailableThemes();
        Theme currentTheme = themeUseCase.getCurrentTheme();
        for (Theme theme : themes) {
            VBox card = createThemeCard(theme, currentTheme);
            themesContainer.getChildren().add(card);
        }
    }
    private VBox createThemeCard(Theme theme, Theme currentTheme) {
        VBox card = new VBox(8);
        card.getStyleClass().add("theme-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(260);
        card.setAlignment(Pos.TOP_LEFT);
        boolean isCurrent = currentTheme != null && currentTheme.id().equals(theme.id());
        if (isCurrent) {
            card.getStyleClass().add("theme-card-active");
        }
        Label nameLabel = new Label(theme.name());
        nameLabel.getStyleClass().add("theme-card-title");
        Label authorLabel = new Label();
        authorLabel.textProperty().bind(i18n.createStringBinding("theme.author.by", theme.author()));
        authorLabel.getStyleClass().add("theme-card-author");
        Label descLabel = new Label(theme.description());
        descLabel.getStyleClass().add("theme-card-description");
        descLabel.setWrapText(true);
        
        FlowPane buttons = new FlowPane(8, 8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        
        Button applyBtn = new Button();
        applyBtn.setText(isCurrent ? i18n.get("theme.preview.badge.active") : "Appliquer");
        applyBtn.getStyleClass().add(isCurrent ? "success-button" : "primary-button");
        applyBtn.setDisable(isCurrent);
        applyBtn.setOnAction(e -> handleApplyTheme(theme.id()));
        buttons.getChildren().add(applyBtn);
        
        if (!theme.isBuiltIn()) {
            Button editBtn = new Button("📝");
            editBtn.getStyleClass().add("primary-button");
            editBtn.setTooltip(new Tooltip(i18n.get("theme.edit.tooltip"))); 
            editBtn.setOnAction(e -> handleEditTheme(theme));
            
            Button exportBtn = new Button("📤");
            exportBtn.getStyleClass().add("secondary-button");
            exportBtn.setTooltip(new Tooltip(i18n.get("theme.export.tooltip")));
            exportBtn.setOnAction(e -> handleExportTheme(theme));
            
            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().add("danger-button");
            deleteBtn.setTooltip(new Tooltip(i18n.get("theme.delete.tooltip")));
            deleteBtn.setOnAction(e -> handleDeleteTheme(theme));
            
            buttons.getChildren().addAll(editBtn, exportBtn, deleteBtn);
        }
        if (theme.isBuiltIn()) {
            Label badge = new Label();
            badge.textProperty().bind(i18n.createStringBinding("theme.preview.badge.builtin"));
            badge.getStyleClass().add("badge-builtin");
            buttons.getChildren().add(badge);
        }
        card.getChildren().addAll(nameLabel, authorLabel, descLabel, buttons);
        return card;
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
                com.app.infrastructure.util.DailyLogger.logWarn("Themes", "User deleting theme: " + theme.name() + " (" + theme.id() + ")");
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
                com.app.infrastructure.util.DailyLogger.logInfo("Themes", "User exporting theme " + theme.id() + " to " + file.getPath());
                themeUseCase.exportTheme(theme.id(), file.toPath());
                setStatus("✅ " + i18n.get("theme.export.success", file.getName()));
            } catch (Exception e) {
                com.app.infrastructure.util.DailyLogger.logError("Themes", "Failed to export theme " + theme.id(), e);
                setStatus("❌ " + i18n.get("theme.export.error", e.getMessage()));
            }
        }
    }
    private void handleEditTheme(Theme theme) {
        java.util.Map<String, String> props = themeUseCase.getThemeProperties(theme.id());
        if (props == null || props.isEmpty()) {
            setStatus("❌ " + i18n.get("theme.edit.error.nodata"));
            return;
        }
        try {
            double radius = 0;
            try { radius = Double.parseDouble(props.getOrDefault("borderRadius", "0")); } catch(Exception e){}
            
            double sideWidth = 250;
            try { sideWidth = Double.parseDouble(props.getOrDefault("sidebarWidth", "250")); } catch(Exception e){}

            Color sideColor = null;
            try { sideColor = Color.web(props.getOrDefault("sidebarColor", "#2b2b2b")); } catch(Exception e){}
            
            ThemeColors colors = new ThemeColors(
                Color.web(props.getOrDefault("background", "#1a1a2e")),
                Color.web(props.getOrDefault("cardBackground", "#16213e")),
                Color.web(props.getOrDefault("primaryText", "#e0e0e0")),
                Color.web(props.getOrDefault("secondaryText", "#888888")),
                Color.web(props.getOrDefault("accent", "#e94560")),
                Color.web(props.getOrDefault("accentHover", "#ff6b7a")),
                Color.web(props.getOrDefault("buttonPrimary", "#0f3460")),
                Color.web(props.getOrDefault("buttonSuccess", "#16c79a")),
                Color.web(props.getOrDefault("buttonWarning", "#f39c12")),
                Color.web(props.getOrDefault("buttonDanger", "#e74c3c")),
                Color.web(props.getOrDefault("inputBackground", "#0f3460")),
                Color.web(props.getOrDefault("inputBorder", "#3d4f6f")),
                Color.web(props.getOrDefault("borderColor", "#3d4f6f")),
                Color.web(props.getOrDefault("scrollbarColor", "#3d4f6f")),
                radius,
                sideWidth,
                sideColor
            );
            showThemeCreatorDialog(theme.name(), theme.author(), colors, true);
        } catch (Exception e) {
            setStatus("❌ " + i18n.get("theme.edit.error.read", e.getMessage()));
        }
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

        Theme currentTheme = themeUseCase.getCurrentTheme();
        ThemeColors initialColors = null;
        
        if (currentTheme != null) {
            java.util.Map<String, String> props = themeUseCase.getThemeProperties(currentTheme.id());
            if (props != null && !props.isEmpty()) {
                try {
                    double radius = 10;
                    try { radius = Double.parseDouble(props.getOrDefault("borderRadius", "10")); } catch(Exception e){}
                    
                    double sideWidth = 250;
                    try { sideWidth = Double.parseDouble(props.getOrDefault("sidebarWidth", "250")); } catch(Exception e){}

                    Color sideColor = Color.web("#2b2b2b");
                    try { sideColor = Color.web(props.getOrDefault("sidebarColor", "#2b2b2b")); } catch(Exception e){}
                    
                    initialColors = new ThemeColors(
                        Color.web(props.getOrDefault("background", "#1a1a2e")),
                        Color.web(props.getOrDefault("cardBackground", "#16213e")),
                        Color.web(props.getOrDefault("primaryText", "#e0e0e0")),
                        Color.web(props.getOrDefault("secondaryText", "#888888")),
                        Color.web(props.getOrDefault("accent", "#e94560")),
                        Color.web(props.getOrDefault("accentHover", "#ff6b7a")),
                        Color.web(props.getOrDefault("buttonPrimary", "#0f3460")),
                        Color.web(props.getOrDefault("buttonSuccess", "#16c79a")),
                        Color.web(props.getOrDefault("buttonWarning", "#f39c12")),
                        Color.web(props.getOrDefault("buttonDanger", "#e74c3c")),
                        Color.web(props.getOrDefault("inputBackground", "#0f3460")),
                        Color.web(props.getOrDefault("inputBorder", "#3d4f6f")),
                        Color.web(props.getOrDefault("borderColor", "#3d4f6f")),
                        Color.web(props.getOrDefault("scrollbarColor", "#3d4f6f")),
                        radius,
                        sideWidth,
                        sideColor
                    );
                } catch (Exception e) {

                }
            }
        }
        
        showThemeCreatorDialog("", "", initialColors, false);
    }
    private record ThemeColors(
        Color background,
        Color cardBackground,
        Color primaryText,
        Color secondaryText,
        Color accent,
        Color accentHover,
        Color buttonPrimary,
        Color buttonSuccess,
        Color buttonWarning,
        Color buttonDanger,
        Color inputBackground,
        Color inputBorder,
        Color borderColor,
        Color scrollbarColor,
        double borderRadius,
        double sidebarWidth,
        Color sidebarColor
    ) {
         java.util.Map<String, String> toMap() {
            java.util.Map<String, String> m = new java.util.HashMap<>();
            m.put("background", toHex(background));
            m.put("cardBackground", toHex(cardBackground));
            m.put("primaryText", toHex(primaryText));
            m.put("secondaryText", toHex(secondaryText));
            m.put("accent", toHex(accent));
            m.put("accentHover", toHex(accentHover));
            m.put("buttonPrimary", toHex(buttonPrimary));
            m.put("buttonSuccess", toHex(buttonSuccess));
            m.put("buttonWarning", toHex(buttonWarning));
            m.put("buttonDanger", toHex(buttonDanger));
            m.put("inputBackground", toHex(inputBackground));
            m.put("inputBorder", toHex(inputBorder));
            m.put("borderColor", toHex(borderColor));
            m.put("scrollbarColor", toHex(scrollbarColor));
            m.put("borderRadius", String.valueOf(borderRadius));
            m.put("sidebarWidth", String.valueOf(sidebarWidth));
            if (sidebarColor != null) {
                m.put("sidebarColor", toHex(sidebarColor));
            }
            return m;
         }
         private String toHex(Color color) {
            return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
        }
    }
    private void showThemeCreatorDialog(String initialName, String initialAuthor, ThemeColors initialColors, boolean isEdit) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? i18n.get("theme.dialog.edit.title") : i18n.get("theme.dialog.create.title"));
        dialog.setHeaderText(i18n.get("theme.dialog.header"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(true);
        TextField nameField = new TextField(initialName);
        nameField.setPromptText(i18n.get("theme.dialog.name.prompt"));
        if (isEdit) nameField.setDisable(true);
        TextField authorField = new TextField(initialAuthor);
        authorField.setPromptText(i18n.get("theme.dialog.author.prompt"));
        ThemeColors defaults = initialColors != null ? initialColors : new ThemeColors(
            Color.web("#1a1a2e"), Color.web("#16213e"), Color.web("#e0e0e0"), Color.web("#888888"),
            Color.web("#e94560"), Color.web("#ff6b7a"), Color.web("#0f3460"), Color.web("#16c79a"),
            Color.web("#f39c12"), Color.web("#e74c3c"), Color.web("#0f3460"), Color.web("#3d4f6f"),
            Color.web("#3d4f6f"), Color.web("#3d4f6f"), 
            10.0, 250.0, Color.web("#2b2b2b")
        );
        ColorPicker bgColor = new ColorPicker(defaults.background);
        ColorPicker cardColor = new ColorPicker(defaults.cardBackground);
        ColorPicker textColor = new ColorPicker(defaults.primaryText);
        ColorPicker mutedTextColor = new ColorPicker(defaults.secondaryText);
        ColorPicker accentColor = new ColorPicker(defaults.accent);
        ColorPicker accentHoverColor = new ColorPicker(defaults.accentHover);
        ColorPicker btnPrimaryColor = new ColorPicker(defaults.buttonPrimary);
        ColorPicker btnSuccessColor = new ColorPicker(defaults.buttonSuccess);
        ColorPicker btnWarningColor = new ColorPicker(defaults.buttonWarning);
        ColorPicker btnDangerColor = new ColorPicker(defaults.buttonDanger);
        ColorPicker inputBgColor = new ColorPicker(defaults.inputBackground);
        ColorPicker inputBorderColor = new ColorPicker(defaults.inputBorder);
        ColorPicker borderColor = new ColorPicker(defaults.borderColor);
        ColorPicker scrollbarColor = new ColorPicker(defaults.scrollbarColor);

        Region bgPreview = createPreviewBox();
        Region cardPreview = createPreviewBox();
        Label textPreview = new Label(i18n.get("theme.preview.sample.text")); textPreview.setStyle("-fx-font-size:14px;");
        Label mutedPreview = new Label(i18n.get("theme.preview.sample.muted")); mutedPreview.setStyle("-fx-font-size:12px;");

        Region accentPreview = createPreviewBox();
        Region accentHoverPreview = createPreviewBox();

        Button btnPrimaryPreview = new Button("Primary");
        Button btnSuccessPreview = new Button("Success");
        Button btnWarningPreview = new Button("Warning");
        Button btnDangerPreview = new Button("Danger");

        TextField inputPreview = new TextField("Input"); inputPreview.setPrefWidth(80);
        Region inputBorderPreview = createPreviewBox();

        Region borderPreview = createPreviewBox();
        Region scrollPreview = createPreviewBox();

        Slider radiusSlider = new Slider(0, 30, defaults.borderRadius);
        radiusSlider.setShowTickLabels(true); radiusSlider.setShowTickMarks(true);
        Slider sidebarWidthSlider = new Slider(150, 400, defaults.sidebarWidth);
        sidebarWidthSlider.setShowTickLabels(true); sidebarWidthSlider.setShowTickMarks(true);
        ColorPicker sidebarColorPicker = new ColorPicker(defaults.sidebarColor != null ? defaults.sidebarColor : Color.web("#2b2b2b"));

        Button radiusPreview = new Button("Radius");
        Region sidebarPreview = new Region(); 
        sidebarPreview.setPrefHeight(40); 
        sidebarPreview.setMinHeight(40);
        
        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: #e74c3c;");
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        GridPane baseGrid = createColorGrid();
        int row = 0;
        baseGrid.add(new Label(i18n.get("theme.name")), 0, row);
        baseGrid.add(nameField, 1, row++);
        baseGrid.add(new Label(i18n.get("theme.author")), 0, row);
        baseGrid.add(authorField, 1, row++);
        baseGrid.add(new Separator(), 0, row++, 3, 1);
        
        baseGrid.add(createSectionLabel(i18n.get("theme.preview.section.colors")), 0, row++, 3, 1);
        addRowWithPreview(baseGrid, row++, "Arrière-plan", bgColor, bgPreview);
        addRowWithPreview(baseGrid, row++, "Cartes/Panneaux", cardColor, cardPreview);
        addRowWithPreview(baseGrid, row++, "Texte principal", textColor, textPreview);
        addRowWithPreview(baseGrid, row++, "Texte secondaire", mutedTextColor, mutedPreview);
        Tab baseTab = new Tab("📋 Base", baseGrid);
        
        GridPane accentGrid = createColorGrid();
        row = 0;
        accentGrid.add(createSectionLabel(i18n.get("theme.preview.section.accent")), 0, row++, 3, 1);
        addRowWithPreview(accentGrid, row++, "Accent principal", accentColor, accentPreview);
        addRowWithPreview(accentGrid, row++, "Accent hover", accentHoverColor, accentHoverPreview);
        
        accentGrid.add(new Separator(), 0, row++, 3, 1);
        accentGrid.add(createSectionLabel(i18n.get("theme.preview.section.buttons")), 0, row++, 3, 1);
        addRowWithPreview(accentGrid, row++, "Bouton principal", btnPrimaryColor, btnPrimaryPreview);
        addRowWithPreview(accentGrid, row++, "Bouton succès", btnSuccessColor, btnSuccessPreview);
        addRowWithPreview(accentGrid, row++, "Bouton warning", btnWarningColor, btnWarningPreview);
        addRowWithPreview(accentGrid, row++, "Bouton danger", btnDangerColor, btnDangerPreview);
        Tab accentTab = new Tab("🎯 Accent & Boutons", accentGrid);
        
        GridPane inputGrid = createColorGrid();
        row = 0;
        inputGrid.add(createSectionLabel(i18n.get("theme.preview.section.inputs")), 0, row++, 3, 1);
        addRowWithPreview(inputGrid, row++, "Fond des inputs", inputBgColor, inputPreview);
        addRowWithPreview(inputGrid, row++, "Bordure des inputs", inputBorderColor, inputBorderPreview);
        
        inputGrid.add(new Separator(), 0, row++, 3, 1);
        inputGrid.add(createSectionLabel(i18n.get("theme.preview.section.ui")), 0, row++, 3, 1);
        addRowWithPreview(inputGrid, row++, "Bordures générales", borderColor, borderPreview);
        addRowWithPreview(inputGrid, row++, "Scrollbar", scrollbarColor, scrollPreview);
        inputGrid.add(validationLabel, 0, row++, 3, 1);
        Tab inputTab = new Tab("⚙️ Inputs & UI", inputGrid);
        
        GridPane layoutGrid = createColorGrid();
        row = 0;
        layoutGrid.add(createSectionLabel(i18n.get("theme.preview.section.layout")), 0, row++, 3, 1);
        addRowWithPreview(layoutGrid, row++, "Rayon (Border Radius)", radiusSlider, radiusPreview);
        
        layoutGrid.add(new Label(i18n.get("theme.preview.layout.sidebar")), 0, row);
        layoutGrid.add(sidebarWidthSlider, 1, row);

        layoutGrid.add(sidebarPreview, 2, row++);
        
        addRowWithPreview(layoutGrid, row++, "Couleur Sidebar", sidebarColorPicker, null);
        
        Tab layoutTab = new Tab("📐 Layout", layoutGrid);

        tabPane.getTabs().addAll(baseTab, accentTab, inputTab, layoutTab);
        ScrollPane previewScroll = new ScrollPane();
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefWidth(280);
        previewScroll.setMinWidth(280);
        previewScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox preview = new VBox(12);
        preview.setPadding(new Insets(15));
        Label previewTitle = new Label(i18n.get("theme.preview.title"));
        previewTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        VBox previewCard = new VBox(6);
        previewCard.setPadding(new Insets(10));
        Label previewCardTitle = new Label(i18n.get("theme.preview.card.title"));
        Label previewCardText = new Label(i18n.get("theme.preview.card.text"));
        previewCard.getChildren().addAll(previewCardTitle, previewCardText);
        Label btnLabel = new Label(i18n.get("theme.preview.buttons"));
        VBox buttonsBox = new VBox(5);
        HBox btnRow1 = new HBox(5);
        Button btnPrimary = new Button("Principal");
        Button btnAccent = new Button("Accent");
        btnRow1.getChildren().addAll(btnPrimary, btnAccent);
        HBox btnRow2 = new HBox(5);
        Button btnSuccess = new Button("✓ Succès");
        Button btnWarning = new Button("⚠ Warning");
        Button btnDanger = new Button("🗑 Danger");
        btnRow2.getChildren().addAll(btnSuccess, btnWarning, btnDanger);
        buttonsBox.getChildren().addAll(btnRow1, btnRow2);
        Label inputLabel = new Label(i18n.get("theme.preview.input"));
        TextField previewInput = new TextField();
        previewInput.setPromptText("Placeholder text...");
        previewInput.setText(i18n.get("theme.preview.input.text"));
        Label textAreaLabel = new Label(i18n.get("theme.preview.textarea"));
        TextArea previewTextArea = new TextArea("Contenu de la zone de texte\nAvec plusieurs lignes");
        previewTextArea.setPrefRowCount(2);
        previewTextArea.setMaxHeight(50);
        Label progressLabel = new Label(i18n.get("theme.preview.progress"));
        ProgressBar previewProgress = new ProgressBar(0.65);
        previewProgress.setPrefWidth(200);
        Label sepLabel = new Label(i18n.get("theme.preview.separator"));
        Separator previewSep = new Separator();
        Label badgeLabel = new Label(i18n.get("theme.preview.badges"));
        HBox badges = new HBox(8);
        Label badge1 = new Label(i18n.get("theme.preview.badge.builtin"));
        Label badge2 = new Label(i18n.get("theme.preview.badge.version"));
        Label badge3 = new Label(i18n.get("theme.preview.badge.active"));
        badges.getChildren().addAll(badge1, badge2, badge3);
        Label listLabel = new Label(i18n.get("theme.preview.list"));
        VBox listItem = new VBox(3);
        listItem.setPadding(new Insets(8));
        Label listItemTitle = new Label(i18n.get("theme.preview.list.selected"));
        listItem.getChildren().add(listItemTitle);
        Label scrollLabel = new Label(i18n.get("theme.preview.scrollbar"));
        preview.getChildren().addAll(
            previewTitle, previewCard, new Separator(),
            btnLabel, buttonsBox, new Separator(),
            inputLabel, previewInput, textAreaLabel, previewTextArea, new Separator(),
            progressLabel, previewProgress, sepLabel, previewSep, new Separator(),
            badgeLabel, badges, listLabel, listItem, scrollLabel
        );
        previewScroll.setContent(preview);
        Runnable updatePreview = () -> {
            String bgHex = toHex(bgColor.getValue());
            String cardHex = toHex(cardColor.getValue());
            String textHex = toHex(textColor.getValue());
            String mutedHex = toHex(mutedTextColor.getValue());
            String accentHex = toHex(accentColor.getValue());
            String btnPrimaryHex = toHex(btnPrimaryColor.getValue());
            String btnSuccessHex = toHex(btnSuccessColor.getValue());
            String btnWarningHex = toHex(btnWarningColor.getValue());
            String btnDangerHex = toHex(btnDangerColor.getValue());
            String inputBgHex = toHex(inputBgColor.getValue());
            String inputBorderHex = toHex(inputBorderColor.getValue());
            String borderHex = toHex(borderColor.getValue());
            String sidebarColorHex = toHex(sidebarColorPicker.getValue());
            double radius = radiusSlider.getValue();

            preview.setStyle("-fx-background-color: " + bgHex + ";");
            previewScroll.setStyle("-fx-background-color: " + bgHex + "; -fx-background: " + bgHex + ";");
            previewTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + textHex + ";");
            previewCard.setStyle("-fx-background-color: " + cardHex + "; -fx-background-radius: 8; -fx-border-color: " + borderHex + "; -fx-border-radius: 8;");
            previewCardTitle.setStyle("-fx-text-fill: " + textHex + "; -fx-font-weight: bold;");
            previewCardText.setStyle("-fx-text-fill: " + mutedHex + ";");
            String labelStyle = "-fx-text-fill: " + textHex + "; -fx-font-size: 12px;";
            btnLabel.setStyle(labelStyle); inputLabel.setStyle(labelStyle); textAreaLabel.setStyle(labelStyle);
            progressLabel.setStyle(labelStyle); sepLabel.setStyle(labelStyle); badgeLabel.setStyle(labelStyle);
            listLabel.setStyle(labelStyle); scrollLabel.setStyle(labelStyle + " -fx-font-style: italic;");
            String btnBase = "-fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 10;";
            btnPrimary.setStyle("-fx-background-color: " + btnPrimaryHex + "; " + btnBase);
            btnAccent.setStyle("-fx-background-color: " + accentHex + "; " + btnBase);
            btnSuccess.setStyle("-fx-background-color: " + btnSuccessHex + "; " + btnBase);
            btnWarning.setStyle("-fx-background-color: " + btnWarningHex + "; " + btnBase);
            btnDanger.setStyle("-fx-background-color: " + btnDangerHex + "; " + btnBase);
            previewInput.setStyle("-fx-background-color: " + inputBgHex + "; -fx-text-fill: " + textHex + 
                "; -fx-border-color: " + inputBorderHex + "; -fx-background-radius: 5; -fx-border-radius: 5;" +
                " -fx-prompt-text-fill: " + mutedHex + ";");
            previewTextArea.setStyle("-fx-control-inner-background: " + inputBgHex + "; -fx-text-fill: " + textHex + ";");
            previewProgress.setStyle("-fx-accent: " + accentHex + "; -fx-control-inner-background: " + btnPrimaryHex + ";");
            previewSep.setStyle("-fx-background-color: " + borderHex + ";");
            badge1.setStyle("-fx-background-color: " + borderHex + "; -fx-text-fill: " + mutedHex + 
                "; -fx-padding: 3 8; -fx-background-radius: 4;");
            badge2.setStyle("-fx-background-color: " + btnPrimaryHex + "; -fx-text-fill: " + accentHex + 
                "; -fx-padding: 2 6; -fx-background-radius: 4;");
            badge3.setStyle("-fx-background-color: " + btnSuccessHex + "; -fx-text-fill: white" + 
                "; -fx-padding: 2 6; -fx-background-radius: 4;");
            listItem.setStyle("-fx-background-color: " + btnPrimaryHex + "; -fx-background-radius: 5;");
            listItemTitle.setStyle("-fx-text-fill: white;");
            validateColors(bgColor.getValue(), textColor.getValue(), validationLabel);

            bgPreview.setStyle("-fx-background-color: " + bgHex + "; -fx-border-color: #888;");
            cardPreview.setStyle("-fx-background-color: " + cardHex + "; -fx-border-color: #888;");
            textPreview.setStyle("-fx-text-fill: " + textHex + ";");
            mutedPreview.setStyle("-fx-text-fill: " + mutedHex + ";");
            
            accentPreview.setStyle("-fx-background-color: " + accentHex + ";");
            accentHoverPreview.setStyle("-fx-background-color: " + toHex(accentHoverColor.getValue()) + ";");
            
            String btnCommon = "-fx-text-fill: white; -fx-background-radius: 6;";
            btnPrimaryPreview.setStyle("-fx-background-color: " + btnPrimaryHex + "; " + btnCommon);
            btnSuccessPreview.setStyle("-fx-background-color: " + btnSuccessHex + "; " + btnCommon);
            btnWarningPreview.setStyle("-fx-background-color: " + btnWarningHex + "; " + btnCommon);
            btnDangerPreview.setStyle("-fx-background-color: " + btnDangerHex + "; " + btnCommon);
            
            inputPreview.setStyle("-fx-background-color: " + inputBgHex + "; -fx-text-fill: " + textHex + "; -fx-border-color: " + inputBorderHex + "; -fx-background-radius: 4;");
            inputBorderPreview.setStyle("-fx-background-color: transparent; -fx-border-color: " + inputBorderHex + "; -fx-border-width: 2;");
            
            borderPreview.setStyle("-fx-background-color: transparent; -fx-border-color: " + borderHex + "; -fx-border-width: 2;");
            scrollPreview.setStyle("-fx-background-color: " + toHex(scrollbarColor.getValue()) + ";");
            
            radiusPreview.setStyle("-fx-background-color: " + btnPrimaryHex + "; -fx-text-fill: white; -fx-background-radius: " + radius + ";");
            
            sidebarPreview.setStyle("-fx-background-color: " + sidebarColorHex + ";");
            sidebarPreview.setPrefWidth(sidebarWidthSlider.getValue() * 0.5);
        };
        
        ColorPicker[] pickers = {bgColor, cardColor, textColor, mutedTextColor, accentColor, accentHoverColor,
            btnPrimaryColor, btnSuccessColor, btnWarningColor, btnDangerColor, inputBgColor, inputBorderColor,
            borderColor, scrollbarColor, sidebarColorPicker};
            
        for (ColorPicker picker : pickers) {
            picker.setOnAction(e -> updatePreview.run());
        }
        radiusSlider.valueProperty().addListener(e -> updatePreview.run());
        sidebarWidthSlider.valueProperty().addListener(e -> updatePreview.run());
        updatePreview.run();
        HBox content = new HBox(15);
        content.getChildren().addAll(tabPane, preview);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ButtonType saveBtnType = new ButtonType(isEdit ? i18n.get("theme.dialog.save") : i18n.get("theme.dialog.create"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(700, 450);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        okButton.setDisable(initialName.isEmpty());
        nameField.textProperty().addListener((obs, old, newVal) -> 
            okButton.setDisable(newVal == null || newVal.trim().isEmpty()));
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtnType) {
            String themeName = nameField.getText().trim();
            if (themeName.isEmpty()) {
                setStatus("❌ " + i18n.get("theme.create.name.required"));
                return;
            }
            String author = authorField.getText().trim();
            if (author.isEmpty()) author = "Custom";
            ThemeColors colors = new ThemeColors(
                bgColor.getValue(), cardColor.getValue(),
                textColor.getValue(), mutedTextColor.getValue(),
                accentColor.getValue(), accentHoverColor.getValue(),
                btnPrimaryColor.getValue(), btnSuccessColor.getValue(),
                btnWarningColor.getValue(), btnDangerColor.getValue(),
                inputBgColor.getValue(), inputBorderColor.getValue(),
                borderColor.getValue(), scrollbarColor.getValue(),
                radiusSlider.getValue(),
                sidebarWidthSlider.getValue(),
                sidebarColorPicker.getValue()
            );
            try {
                String newThemeId = createCustomTheme(themeName, author, colors);
                refreshThemesList();
                
                Theme current = themeUseCase.getCurrentTheme();
                if (current != null && current.id().equals(newThemeId)) {
                     handleApplyTheme(newThemeId);
                }
                
                String action = isEdit ? i18n.get("theme.action.modified") : i18n.get("theme.action.created");
                setStatus("✅ " + i18n.get("theme.create.success", themeName, action));
            } catch (IOException e) {
                setStatus("❌ Erreur : " + e.getMessage());
                com.app.infrastructure.util.DailyLogger.logError("Themes", "Failed to create theme", e);
            }
        }
    }
    private GridPane createColorGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        return grid;
    }
    
    private void addRowWithPreview(GridPane grid, int row, String labelText, javafx.scene.Node control, javafx.scene.Node preview) {
        grid.add(new Label(labelText), 0, row);
        grid.add(control, 1, row);
        if (preview != null) {
            grid.add(preview, 2, row);
        }
    }
    
    private Region createPreviewBox() {
        Region r = new Region();
        r.setPrefSize(30, 20);
        r.setMinSize(30, 20);
        r.setStyle("-fx-border-color: #888; -fx-border-width: 1;");
        return r;
    }
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        return label;
    }
    private void validateColors(Color bg, Color text, Label validationLabel) {
        StringBuilder warnings = new StringBuilder();
        if (bg.getOpacity() < 0.3) warnings.append("⚠ Arrière-plan trop transparent\n");
        if (text.getOpacity() < 0.5) warnings.append("⚠ Texte trop transparent\n");
        double contrast = Math.abs(getBrightness(bg) - getBrightness(text));
        if (contrast < 0.3) warnings.append("⚠ Faible contraste texte/fond\n");
        validationLabel.setText(warnings.toString());
    }
    private double getBrightness(Color color) {
        return (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114) * color.getOpacity();
    }
    private String createCustomTheme(String name, String author, ThemeColors c) throws IOException {
        String themeId = "custom-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        com.app.infrastructure.util.DailyLogger.logInfo("Themes", "User creating custom theme: " + themeId + " (" + name + ")");
        Path themeDir = Paths.get("themes", "custom", themeId);
        Files.createDirectories(themeDir);
        String css = generateFullThemeCss(c);
        Files.writeString(themeDir.resolve("theme.css"), css);
        String colorsJson = c.toMap().entrySet().stream()
            .map(e -> String.format("\"%s\": \"%s\"", e.getKey(), e.getValue()))
            .collect(java.util.stream.Collectors.joining(",\n    "));
        String manifest = String.format("""
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
            """, themeId, name, author, colorsJson);
        Files.writeString(themeDir.resolve("theme.json"), manifest);
        com.app.infrastructure.util.DailyLogger.logInfo("Themes", "Custom theme created at: " + themeDir.toAbsolutePath());
        return themeId;
    }
    private String generateFullThemeCss(ThemeColors c) {
        return String.format("""
            .root {
                -fx-base: %s;
                -fx-control-inner-background: %s;
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-accent: %s;
                -fx-focus-color: %s;
            }
            .label { -fx-text-fill: %s; -fx-font-size: 14px; }
            .section-title { -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .card-title { -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .text-muted { -fx-text-fill: %s; -fx-font-size: 12px; }
            .button {
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-background-radius: 6;
                -fx-padding: 8 16;
                -fx-cursor: hand;
            }
            .button:hover { -fx-background-color: %s; }
            .primary-button { -fx-background-color: %s; -fx-text-fill: white; }
            .primary-button:hover { -fx-background-color: %s; }
            .success-button { -fx-background-color: %s; -fx-text-fill: white; }
            .success-button:hover { -fx-background-color: %s; }
            .warning-button { -fx-background-color: %s; -fx-text-fill: white; }
            .warning-button:hover { -fx-background-color: %s; }
            .danger-button { -fx-background-color: %s; -fx-text-fill: white; }
            .danger-button:hover { -fx-background-color: %s; }
            .secondary-button { -fx-background-color: %s; -fx-text-fill: white; }
            .card {
                -fx-background-color: %s;
                -fx-background-radius: 10;
                -fx-padding: 15;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);
            }
            .info-card {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 10;
                -fx-border-width: 1;
            }
            .theme-card, .plugin-card {
                -fx-background-color: %s;
                -fx-background-radius: 10;
                -fx-border-color: %s;
                -fx-border-radius: 10;
                -fx-border-width: 1;
            }
            .theme-card:hover, .plugin-card:hover { -fx-border-color: %s; }
            .theme-card-active, .plugin-card-active { -fx-border-color: %s; -fx-border-width: 2; }
            .plugin-card-disabled { -fx-opacity: 0.6; }
            .theme-card-title, .plugin-card-title { -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .theme-card-author, .plugin-card-author { -fx-font-size: 12px; -fx-text-fill: %s; }
            .theme-card-description, .plugin-card-description { -fx-font-size: 13px; -fx-text-fill: %s; }
            .badge-builtin { -fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; }
            .version-badge { -fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px; }
            .status-badge { -fx-font-size: 12px; -fx-text-fill: %s; }
            .text-field {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-prompt-text-fill: %s;
                -fx-background-radius: 6;
                -fx-border-color: %s;
                -fx-border-radius: 6;
                -fx-padding: 8;
            }
            .text-field:focused { -fx-border-color: %s; }
            .text-area {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-family: 'Consolas', 'Monaco', monospace;
                -fx-font-size: 13px;
            }
            .text-area .content { -fx-background-color: %s; }
            .log-area { -fx-background-color: %s; }
            .log-area .content { -fx-background-color: %s; }
            .progress-bar { -fx-accent: %s; -fx-control-inner-background: %s; }
            .progress-bar .bar { -fx-background-color: linear-gradient(to right, %s, %s); -fx-background-radius: 5; }
            .progress-bar .track { -fx-background-color: %s; -fx-background-radius: 5; }
            .online-toggle { -fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 15; -fx-padding: 5 12; }
            .online-toggle:selected { -fx-background-color: %s; -fx-text-fill: white; }
            .scroll-pane, .scroll-pane .viewport { -fx-background-color: transparent; }
            .scroll-bar { -fx-background-color: transparent; }
            .scroll-bar .thumb { -fx-background-color: %s; -fx-background-radius: 4; }
            .scroll-bar .thumb:hover { -fx-background-color: %s; }
            .separator { -fx-background-color: %s; }
            .separator .line { -fx-border-color: %s; -fx-border-width: 1 0 0 0; }
            .list-view { -fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 6; }
            .list-cell { -fx-background-color: transparent; -fx-text-fill: %s; -fx-padding: 8; }
            .list-cell:filled:selected { -fx-background-color: %s; -fx-text-fill: white; }
            .list-cell:filled:hover { -fx-background-color: %s; }
            .status-label { -fx-text-fill: %s; -fx-font-size: 13px; }
            .current-theme-label { -fx-text-fill: %s; -fx-font-weight: bold; }
            .count-label { -fx-text-fill: %s; -fx-font-size: 13px; }
            .empty-message { -fx-text-fill: %s; -fx-font-size: 14px; -fx-text-alignment: center; }
            .themes-container, .plugins-container { -fx-padding: 10; }
            """,
            toHex(c.background), toHex(c.cardBackground), toHex(c.background),
            toHex(c.primaryText), toHex(c.buttonPrimary), toHex(c.accent),
            toHex(c.primaryText), toHex(c.primaryText), toHex(c.primaryText), toHex(c.secondaryText),
            toHex(c.buttonPrimary), toHex(c.buttonPrimary.brighter()),
            toHex(c.accent), toHex(c.accentHover),
            toHex(c.buttonSuccess), toHex(c.buttonSuccess.brighter()),
            toHex(c.buttonWarning), toHex(c.buttonWarning.brighter()),
            toHex(c.buttonDanger), toHex(c.buttonDanger.brighter()),
            toHex(c.borderColor),
            toHex(c.cardBackground),
            toHex(c.buttonPrimary), toHex(c.accent),
            toHex(c.cardBackground), toHex(c.borderColor), toHex(c.accent), toHex(c.buttonSuccess),
            toHex(c.primaryText), toHex(c.secondaryText), toHex(c.secondaryText),
            toHex(c.borderColor), toHex(c.secondaryText), toHex(c.buttonPrimary), toHex(c.accent), toHex(c.secondaryText),
            toHex(c.inputBackground), toHex(c.primaryText), toHex(c.secondaryText), toHex(c.inputBorder), toHex(c.accent),
            toHex(c.inputBackground), toHex(c.primaryText), toHex(c.inputBackground),
            toHex(c.background.darker()), toHex(c.background.darker()),
            toHex(c.accent), toHex(c.buttonPrimary), toHex(c.accent), toHex(c.accentHover), toHex(c.buttonPrimary),
            toHex(c.cardBackground), toHex(c.buttonSuccess), toHex(c.buttonSuccess),
            toHex(c.scrollbarColor), toHex(c.scrollbarColor.brighter()),
            toHex(c.borderColor), toHex(c.borderColor),
            toHex(c.cardBackground), toHex(c.borderColor), toHex(c.primaryText), toHex(c.buttonPrimary), toHex(c.cardBackground.brighter()),
            toHex(c.secondaryText), toHex(c.buttonSuccess), toHex(c.secondaryText), toHex(c.secondaryText)
        );
    }
    private String toHex(Color color) {
        return String.format("#%02x%02x%02x",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
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
