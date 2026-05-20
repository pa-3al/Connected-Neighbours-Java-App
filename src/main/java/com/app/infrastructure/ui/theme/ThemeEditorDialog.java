package com.app.infrastructure.ui.theme;

import java.util.Optional;

import com.app.infrastructure.i18n.I18nService;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ThemeEditorDialog {

    private final I18nService i18n;

    public ThemeEditorDialog(I18nService i18n) {
        this.i18n = i18n;
    }

    public Optional<ThemeDraft> show(
            Window owner,
            String initialName,
            String initialAuthor,
            ThemeColors initialColors,
            boolean isEdit
    ) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(isEdit ? i18n.get("theme.dialog.edit.title") : i18n.get("theme.dialog.create.title"));
        dialog.setHeaderText(i18n.get("theme.dialog.header"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setResizable(true);
        dialog.getDialogPane().setStyle("-fx-background-radius: 18; -fx-border-radius: 18;");

        TextField nameField = new TextField(initialName);
        nameField.setPromptText(i18n.get("theme.dialog.name.prompt"));
        if (isEdit) {
            nameField.setDisable(true);
        }

        TextField authorField = new TextField(initialAuthor);
        authorField.setPromptText(i18n.get("theme.dialog.author.prompt"));

        ColorPicker bgColor = new ColorPicker(initialColors.background());
        ColorPicker cardColor = new ColorPicker(initialColors.cardBackground());
        ColorPicker textColor = new ColorPicker(initialColors.primaryText());
        ColorPicker mutedTextColor = new ColorPicker(initialColors.secondaryText());
        ColorPicker accentColor = new ColorPicker(initialColors.accent());
        ColorPicker accentHoverColor = new ColorPicker(initialColors.accentHover());
        ColorPicker btnPrimaryColor = new ColorPicker(initialColors.buttonPrimary());
        ColorPicker btnSuccessColor = new ColorPicker(initialColors.buttonSuccess());
        ColorPicker btnWarningColor = new ColorPicker(initialColors.buttonWarning());
        ColorPicker btnDangerColor = new ColorPicker(initialColors.buttonDanger());
        ColorPicker inputBgColor = new ColorPicker(initialColors.inputBackground());
        ColorPicker inputBorderColor = new ColorPicker(initialColors.inputBorder());
        ColorPicker borderColor = new ColorPicker(initialColors.borderColor());
        ColorPicker scrollbarColor = new ColorPicker(initialColors.scrollbarColor());
        ColorPicker chartColor1 = new ColorPicker(initialColors.chartColor1());
        ColorPicker chartColor2 = new ColorPicker(initialColors.chartColor2());
        ColorPicker chartColor3 = new ColorPicker(initialColors.chartColor3());
        ColorPicker chartColor4 = new ColorPicker(initialColors.chartColor4());
        ColorPicker chartColor5 = new ColorPicker(initialColors.chartColor5());
        ColorPicker chartColor6 = new ColorPicker(initialColors.chartColor6());
        ColorPicker chartColor7 = new ColorPicker(initialColors.chartColor7());
        ColorPicker chartColor8 = new ColorPicker(initialColors.chartColor8());

        Region bgPreview = createPreviewBox();
        Region cardPreview = createPreviewBox();
        Label textPreview = new Label(i18n.get("theme.preview.sample.text"));
        textPreview.setStyle("-fx-font-size:14px;");
        Label mutedPreview = new Label(i18n.get("theme.preview.sample.muted"));
        mutedPreview.setStyle("-fx-font-size:12px;");

        Region accentPreview = createPreviewBox();
        Region accentHoverPreview = createPreviewBox();

        Button btnPrimaryPreview = new Button("Primary");
        Button btnSuccessPreview = new Button("Success");
        Button btnWarningPreview = new Button("Warning");
        Button btnDangerPreview = new Button("Danger");

        TextField inputPreview = new TextField("Input");
        inputPreview.setPrefWidth(80);
        Region inputBorderPreview = createPreviewBox();

        Region borderPreview = createPreviewBox();
        Region scrollPreview = createPreviewBox();
        Region chartPreview1 = createPreviewBox();
        Region chartPreview2 = createPreviewBox();
        Region chartPreview3 = createPreviewBox();
        Region chartPreview4 = createPreviewBox();
        Region chartPreview5 = createPreviewBox();
        Region chartPreview6 = createPreviewBox();
        Region chartPreview7 = createPreviewBox();
        Region chartPreview8 = createPreviewBox();

        Slider radiusSlider = new Slider(0, 30, initialColors.borderRadius());
        radiusSlider.setShowTickLabels(false);
        radiusSlider.setShowTickMarks(false);
        radiusSlider.setPrefWidth(180);
        radiusSlider.setMaxWidth(180);
        Label radiusValueLabel = new Label();

        Slider sidebarWidthSlider = new Slider(150, 400, initialColors.sidebarWidth());
        sidebarWidthSlider.setShowTickLabels(false);
        sidebarWidthSlider.setShowTickMarks(false);
        sidebarWidthSlider.setPrefWidth(180);
        sidebarWidthSlider.setMaxWidth(180);
        Label sidebarWidthValueLabel = new Label();

        ColorPicker sidebarColorPicker = new ColorPicker(
                initialColors.sidebarColor() != null ? initialColors.sidebarColor() : Color.web("#2b2b2b")
        );

        Button radiusPreview = new Button("Radius");
        radiusPreview.setMaxWidth(120);
        Region sidebarPreview = new Region();
        sidebarPreview.setPrefHeight(40);
        sidebarPreview.setMinHeight(40);
        sidebarPreview.setMinWidth(45);
        sidebarPreview.setMaxWidth(120);

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
        Tab baseTab = new Tab("Base", baseGrid);

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
        Tab accentTab = new Tab("Accent", accentGrid);

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
        Tab inputTab = new Tab("UI", inputGrid);

        GridPane chartGrid = createColorGrid();
        row = 0;
        chartGrid.add(createSectionLabel("Couleurs des graphiques"), 0, row++, 3, 1);
        addRowWithPreview(chartGrid, row++, "Chart 1", chartColor1, chartPreview1);
        addRowWithPreview(chartGrid, row++, "Chart 2", chartColor2, chartPreview2);
        addRowWithPreview(chartGrid, row++, "Chart 3", chartColor3, chartPreview3);
        addRowWithPreview(chartGrid, row++, "Chart 4", chartColor4, chartPreview4);
        addRowWithPreview(chartGrid, row++, "Chart 5", chartColor5, chartPreview5);
        addRowWithPreview(chartGrid, row++, "Chart 6", chartColor6, chartPreview6);
        addRowWithPreview(chartGrid, row++, "Chart 7", chartColor7, chartPreview7);
        addRowWithPreview(chartGrid, row++, "Chart 8", chartColor8, chartPreview8);
        chartGrid.add(new Separator(), 0, row++, 3, 1);
        BarChart<String, Number> editorBarChart = createBarChartPreview();
        PieChart editorPieChart = createPieChartPreview();
        chartGrid.add(editorBarChart, 0, row++, 3, 1);
        chartGrid.add(editorPieChart, 0, row++, 3, 1);
        Tab chartTab = new Tab("Charts", chartGrid);

        GridPane layoutGrid = createColorGrid();
        row = 0;
        layoutGrid.add(createSectionLabel(i18n.get("theme.preview.section.layout")), 0, row++, 3, 1);
        addLayoutSliderRow(layoutGrid, row++, "Rayon (Border Radius)", radiusSlider, radiusValueLabel, radiusPreview);

        layoutGrid.add(new Label(i18n.get("theme.preview.layout.sidebar")), 0, row);
        HBox sidebarWidthBox = createSliderBox(sidebarWidthSlider, sidebarWidthValueLabel);
        layoutGrid.add(sidebarWidthBox, 1, row);
        layoutGrid.add(sidebarPreview, 2, row++);

        addRowWithPreview(layoutGrid, row++, "Couleur Sidebar", sidebarColorPicker, null);
        Tab layoutTab = new Tab("Layout", layoutGrid);

        tabPane.getTabs().addAll(baseTab, accentTab, inputTab, chartTab, layoutTab);

        ScrollPane previewScroll = new ScrollPane();
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefViewportWidth(360);
        previewScroll.setMinViewportWidth(280);
        previewScroll.setMinWidth(280);
        previewScroll.setMaxWidth(Double.MAX_VALUE);
        previewScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox preview = new VBox(12);
        preview.setPadding(new Insets(15));
        preview.setMinWidth(0);
        preview.setMaxWidth(Double.MAX_VALUE);
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
        Button btnSuccess = new Button("Succes");
        Button btnWarning = new Button("Warning");
        Button btnDanger = new Button("Danger");
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
        Label chartLabel = new Label("Charts");
        BarChart<String, Number> previewBarChart = createBarChartPreview();
        PieChart previewPieChart = createPieChartPreview();

        preview.getChildren().addAll(
                previewTitle, previewCard, new Separator(),
                btnLabel, buttonsBox, new Separator(),
                inputLabel, previewInput, textAreaLabel, previewTextArea, new Separator(),
                progressLabel, previewProgress, sepLabel, previewSep, new Separator(),
                badgeLabel, badges, listLabel, listItem, chartLabel, previewBarChart, previewPieChart, scrollLabel
        );

        VBox focusedPreview = new VBox(12);
        focusedPreview.setPadding(new Insets(15));
        focusedPreview.setMinWidth(0);
        focusedPreview.setMaxWidth(Double.MAX_VALUE);
        Label focusedTitle = new Label("Aperçu ciblé");
        focusedTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox focusedCard = new VBox(6);
        focusedCard.setPadding(new Insets(10));
        Label focusedCardTitle = new Label(i18n.get("theme.preview.card.title"));
        Label focusedCardText = new Label(i18n.get("theme.preview.card.text"));
        focusedCard.getChildren().addAll(focusedCardTitle, focusedCardText);

        VBox focusedButtons = new VBox(8);
        Button focusedPrimary = new Button("Principal");
        Button focusedAccent = new Button("Accent");
        Button focusedSuccess = new Button("Succes");
        Button focusedWarning = new Button("Warning");
        Button focusedDanger = new Button("Danger");
        focusedButtons.getChildren().addAll(
                new HBox(6, focusedPrimary, focusedAccent),
                new HBox(6, focusedSuccess, focusedWarning, focusedDanger)
        );

        VBox focusedInputs = new VBox(8);
        TextField focusedInput = new TextField(i18n.get("theme.preview.input.text"));
        TextArea focusedTextArea = new TextArea("Contenu de la zone de texte\nAvec plusieurs lignes");
        focusedTextArea.setPrefRowCount(3);
        focusedInputs.getChildren().addAll(focusedInput, focusedTextArea);

        HBox focusedLayout = new HBox(10);
        Region focusedSidebar = new Region();
        focusedSidebar.setPrefSize(70, 140);
        focusedSidebar.setMinSize(50, 120);
        VBox focusedSurface = new VBox(8);
        focusedSurface.setPadding(new Insets(10));
        Button focusedRadius = new Button("Radius");
        focusedSurface.getChildren().addAll(new Label("Surface"), focusedRadius);
        HBox.setHgrow(focusedSurface, Priority.ALWAYS);
        focusedLayout.getChildren().addAll(focusedSidebar, focusedSurface);

        VBox focusedCharts = new VBox(8);
        BarChart<String, Number> focusedBarChart = createBarChartPreview();
        PieChart focusedPieChart = createPieChartPreview();
        focusedCharts.getChildren().addAll(focusedBarChart, focusedPieChart);

        focusedPreview.getChildren().addAll(
                focusedTitle, focusedCard, focusedButtons, focusedInputs, focusedLayout, focusedCharts
        );

        ToggleButton generalPreviewToggle = new ToggleButton("Général");
        ToggleButton focusedPreviewToggle = new ToggleButton("Ciblé");
        ToggleGroup previewModeGroup = new ToggleGroup();
        generalPreviewToggle.setToggleGroup(previewModeGroup);
        focusedPreviewToggle.setToggleGroup(previewModeGroup);
        generalPreviewToggle.setMinWidth(92);
        focusedPreviewToggle.setMinWidth(92);
        previewModeGroup.selectToggle(generalPreviewToggle);

        HBox previewModeBar = new HBox(6, generalPreviewToggle, focusedPreviewToggle);
        previewModeBar.setAlignment(Pos.CENTER);
        previewModeBar.setMaxWidth(Double.MAX_VALUE);

        StackPane previewStack = new StackPane(preview, focusedPreview);
        previewStack.setAlignment(Pos.TOP_LEFT);
        previewStack.setMaxWidth(Double.MAX_VALUE);
        VBox previewModeContainer = new VBox(0, previewModeBar, previewStack);
        previewModeContainer.setMaxWidth(Double.MAX_VALUE);
        setVisibleManaged(focusedPreview, false);
        previewScroll.setContent(previewModeContainer);

        Runnable updateFocusedPreview = () -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            boolean baseSelected = selectedTab == baseTab;
            boolean accentSelected = selectedTab == accentTab;
            boolean inputSelected = selectedTab == inputTab;
            boolean chartSelected = selectedTab == chartTab;
            boolean layoutSelected = selectedTab == layoutTab;

            focusedTitle.setText(
                    baseSelected ? "Aperçu ciblé - Base" :
                    accentSelected ? "Aperçu ciblé - Accent" :
                    inputSelected ? "Aperçu ciblé - UI" :
                    chartSelected ? "Aperçu ciblé - Charts" :
                    "Aperçu ciblé - Layout"
            );
            setVisibleManaged(focusedCard, baseSelected);
            setVisibleManaged(focusedButtons, accentSelected);
            setVisibleManaged(focusedInputs, inputSelected);
            setVisibleManaged(focusedCharts, chartSelected);
            setVisibleManaged(focusedLayout, layoutSelected);
        };

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
            String chartHex1 = toHex(chartColor1.getValue());
            String chartHex2 = toHex(chartColor2.getValue());
            String chartHex3 = toHex(chartColor3.getValue());
            String chartHex4 = toHex(chartColor4.getValue());
            String chartHex5 = toHex(chartColor5.getValue());
            String chartHex6 = toHex(chartColor6.getValue());
            String chartHex7 = toHex(chartColor7.getValue());
            String chartHex8 = toHex(chartColor8.getValue());
            double radius = radiusSlider.getValue();
            radiusValueLabel.setText(String.format("%.0f px", radius));
            sidebarWidthValueLabel.setText(String.format("%.0f px", sidebarWidthSlider.getValue()));
            radiusValueLabel.setStyle("-fx-text-fill: " + textHex + ";");
            sidebarWidthValueLabel.setStyle("-fx-text-fill: " + textHex + ";");

            preview.setStyle("-fx-background-color: " + bgHex + ";");
            previewScroll.setStyle("-fx-background-color: " + bgHex + "; -fx-background: " + bgHex + ";");
            previewModeContainer.setStyle("-fx-background-color: " + bgHex + ";");
            previewModeBar.setStyle("-fx-background-color: " + bgHex + "; -fx-padding: 10 0 8 0;");
            previewStack.setStyle("-fx-background-color: " + bgHex + ";");
            previewTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + textHex + ";");
            previewCard.setStyle("-fx-background-color: " + cardHex + "; -fx-background-radius: 8; -fx-border-color: " + borderHex + "; -fx-border-radius: 8;");
            previewCardTitle.setStyle("-fx-text-fill: " + textHex + "; -fx-font-weight: bold;");
            previewCardText.setStyle("-fx-text-fill: " + mutedHex + ";");
            focusedPreview.setStyle("-fx-background-color: " + bgHex + ";");
            focusedTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + textHex + ";");
            focusedCard.setStyle("-fx-background-color: " + cardHex + "; -fx-background-radius: " + radius + "; -fx-border-color: " + borderHex + "; -fx-border-radius: " + radius + ";");
            focusedCardTitle.setStyle("-fx-text-fill: " + textHex + "; -fx-font-weight: bold;");
            focusedCardText.setStyle("-fx-text-fill: " + mutedHex + ";");
            focusedLayout.setStyle("-fx-background-color: " + bgHex + ";");
            focusedSidebar.setStyle("-fx-background-color: " + sidebarColorHex + "; -fx-background-radius: " + radius + ";");
            focusedSurface.setStyle("-fx-background-color: " + cardHex + "; -fx-background-radius: " + radius + "; -fx-border-color: " + borderHex + "; -fx-border-radius: " + radius + ";");
            stylePreviewToggle(generalPreviewToggle, generalPreviewToggle.isSelected(), accentHex, cardHex, mutedHex, borderHex);
            stylePreviewToggle(focusedPreviewToggle, focusedPreviewToggle.isSelected(), accentHex, cardHex, mutedHex, borderHex);

            String labelStyle = "-fx-text-fill: " + textHex + "; -fx-font-size: 12px;";
            btnLabel.setStyle(labelStyle);
            inputLabel.setStyle(labelStyle);
            textAreaLabel.setStyle(labelStyle);
            progressLabel.setStyle(labelStyle);
            sepLabel.setStyle(labelStyle);
            badgeLabel.setStyle(labelStyle);
            listLabel.setStyle(labelStyle);
            chartLabel.setStyle(labelStyle);
            scrollLabel.setStyle(labelStyle + " -fx-font-style: italic;");

            String btnBase = "-fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 10;";
            btnPrimary.setStyle("-fx-background-color: " + btnPrimaryHex + "; " + btnBase);
            btnAccent.setStyle("-fx-background-color: " + accentHex + "; " + btnBase);
            btnSuccess.setStyle("-fx-background-color: " + btnSuccessHex + "; " + btnBase);
            btnWarning.setStyle("-fx-background-color: " + btnWarningHex + "; " + btnBase);
            btnDanger.setStyle("-fx-background-color: " + btnDangerHex + "; " + btnBase);
            focusedPrimary.setStyle("-fx-background-color: " + btnPrimaryHex + "; " + btnBase);
            focusedAccent.setStyle("-fx-background-color: " + accentHex + "; " + btnBase);
            focusedSuccess.setStyle("-fx-background-color: " + btnSuccessHex + "; " + btnBase);
            focusedWarning.setStyle("-fx-background-color: " + btnWarningHex + "; " + btnBase);
            focusedDanger.setStyle("-fx-background-color: " + btnDangerHex + "; " + btnBase);
            focusedRadius.setStyle("-fx-background-color: " + btnPrimaryHex + "; " + btnBase + " -fx-background-radius: " + radius + ";");

            previewInput.setStyle("-fx-background-color: " + inputBgHex + "; -fx-text-fill: " + textHex
                    + "; -fx-border-color: " + inputBorderHex + "; -fx-background-radius: 5; -fx-border-radius: 5;"
                    + " -fx-prompt-text-fill: " + mutedHex + ";");
            previewTextArea.setStyle("-fx-control-inner-background: " + inputBgHex + "; -fx-text-fill: " + textHex + ";");
            focusedInput.setStyle("-fx-background-color: " + inputBgHex + "; -fx-text-fill: " + textHex
                    + "; -fx-border-color: " + inputBorderHex + "; -fx-background-radius: " + radius + "; -fx-border-radius: " + radius + ";"
                    + " -fx-prompt-text-fill: " + mutedHex + ";");
            focusedTextArea.setStyle("-fx-control-inner-background: " + inputBgHex + "; -fx-text-fill: " + textHex
                    + "; -fx-border-color: " + inputBorderHex + "; -fx-background-radius: " + radius + "; -fx-border-radius: " + radius + ";");
            previewProgress.setStyle("-fx-accent: " + accentHex + "; -fx-control-inner-background: " + btnPrimaryHex + ";");
            previewSep.setStyle("-fx-background-color: " + borderHex + ";");

            badge1.setStyle("-fx-background-color: " + borderHex + "; -fx-text-fill: " + mutedHex + "; -fx-padding: 3 8; -fx-background-radius: 4;");
            badge2.setStyle("-fx-background-color: " + btnPrimaryHex + "; -fx-text-fill: " + accentHex + "; -fx-padding: 2 6; -fx-background-radius: 4;");
            badge3.setStyle("-fx-background-color: " + btnSuccessHex + "; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4;");

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
            chartPreview1.setStyle("-fx-background-color: " + chartHex1 + "; -fx-border-color: " + borderHex + ";");
            chartPreview2.setStyle("-fx-background-color: " + chartHex2 + "; -fx-border-color: " + borderHex + ";");
            chartPreview3.setStyle("-fx-background-color: " + chartHex3 + "; -fx-border-color: " + borderHex + ";");
            chartPreview4.setStyle("-fx-background-color: " + chartHex4 + "; -fx-border-color: " + borderHex + ";");
            chartPreview5.setStyle("-fx-background-color: " + chartHex5 + "; -fx-border-color: " + borderHex + ";");
            chartPreview6.setStyle("-fx-background-color: " + chartHex6 + "; -fx-border-color: " + borderHex + ";");
            chartPreview7.setStyle("-fx-background-color: " + chartHex7 + "; -fx-border-color: " + borderHex + ";");
            chartPreview8.setStyle("-fx-background-color: " + chartHex8 + "; -fx-border-color: " + borderHex + ";");
            applyChartPreviewStyle(previewBarChart, previewPieChart, bgHex, textHex, borderHex,
                    chartHex1, chartHex2, chartHex3, chartHex4,
                    chartHex5, chartHex6, chartHex7, chartHex8);
            applyChartPreviewStyle(focusedBarChart, focusedPieChart, bgHex, textHex, borderHex,
                    chartHex1, chartHex2, chartHex3, chartHex4,
                    chartHex5, chartHex6, chartHex7, chartHex8);
            applyChartPreviewStyle(editorBarChart, editorPieChart, bgHex, textHex, borderHex,
                    chartHex1, chartHex2, chartHex3, chartHex4,
                    chartHex5, chartHex6, chartHex7, chartHex8);

            radiusPreview.setStyle("-fx-background-color: " + btnPrimaryHex + "; -fx-text-fill: white; -fx-background-radius: " + radius + ";");
            sidebarPreview.setStyle("-fx-background-color: " + sidebarColorHex + ";");
            sidebarPreview.setPrefWidth(45 + ((sidebarWidthSlider.getValue() - 150) / 250) * 75);
        };

        ColorPicker[] pickers = {
                bgColor, cardColor, textColor, mutedTextColor, accentColor, accentHoverColor,
                btnPrimaryColor, btnSuccessColor, btnWarningColor, btnDangerColor, inputBgColor, inputBorderColor,
                borderColor, scrollbarColor, sidebarColorPicker,
                chartColor1, chartColor2, chartColor3, chartColor4,
                chartColor5, chartColor6, chartColor7, chartColor8
        };

        for (ColorPicker picker : pickers) {
            picker.setOnAction(e -> updatePreview.run());
        }
        radiusSlider.valueProperty().addListener(e -> updatePreview.run());
        sidebarWidthSlider.valueProperty().addListener(e -> updatePreview.run());
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> updateFocusedPreview.run());
        previewModeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    previewModeGroup.selectToggle(oldToggle);
                }
                return;
            }
            boolean focusedSelected = newToggle == focusedPreviewToggle;
            setVisibleManaged(preview, !focusedSelected);
            setVisibleManaged(focusedPreview, focusedSelected);
            updatePreview.run();
        });
        updatePreview.run();
        updateFocusedPreview.run();

        ScrollPane editorScroll = new ScrollPane(tabPane);
        editorScroll.setFitToWidth(true);
        editorScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        editorScroll.setPrefViewportWidth(560);
        editorScroll.setMinViewportWidth(360);
        editorScroll.setMaxWidth(Double.MAX_VALUE);
        tabPane.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editorScroll, Priority.ALWAYS);
        HBox.setHgrow(previewScroll, Priority.ALWAYS);

        HBox content = new HBox(15);
        content.getChildren().addAll(editorScroll, previewScroll);
        content.setPadding(new Insets(10));
        content.setFillHeight(true);

        dialog.getDialogPane().setContent(content);
        ButtonType saveBtnType = new ButtonType(
                isEdit ? i18n.get("theme.dialog.save") : i18n.get("theme.dialog.create"),
                ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        double prefWidth = owner != null && owner.getWidth() > 0 ? Math.min(1100, Math.max(820, owner.getWidth() * 0.8)) : 1000;
        double prefHeight = owner != null && owner.getHeight() > 0 ? Math.min(720, Math.max(560, owner.getHeight() * 0.8)) : 650;
        dialog.getDialogPane().setPrefSize(prefWidth, prefHeight);
        dialog.setOnShown(event -> {
            Scene scene = dialog.getDialogPane().getScene();
            if (scene != null) {
                scene.setFill(Color.TRANSPARENT);
            }
            Node headerPanel = dialog.getDialogPane().lookup(".header-panel");
            if (headerPanel != null) {
                headerPanel.setStyle("-fx-background-radius: 18 18 0 0;");
                enableWindowDrag(headerPanel);
            }
            Node buttonBar = dialog.getDialogPane().lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.setStyle("-fx-background-radius: 0 0 18 18;");
            }
        });

        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        okButton.setDisable(initialName.isEmpty());
        nameField.textProperty().addListener(
                (obs, oldValue, newVal) -> okButton.setDisable(newVal == null || newVal.trim().isEmpty())
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveBtnType) {
            return Optional.empty();
        }

        String themeName = nameField.getText().trim();
        if (themeName.isEmpty()) {
            return Optional.empty();
        }

        String author = authorField.getText().trim();
        if (author.isEmpty()) {
            author = "Custom";
        }

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
                sidebarColorPicker.getValue(),
                chartColor1.getValue(), chartColor2.getValue(),
                chartColor3.getValue(), chartColor4.getValue(),
                chartColor5.getValue(), chartColor6.getValue(),
                chartColor7.getValue(), chartColor8.getValue()
        );

        return Optional.of(new ThemeDraft(themeName, author, colors));
    }

    private GridPane createColorGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        grid.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);
        ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setMinWidth(180);
        controlColumn.setHgrow(Priority.ALWAYS);
        controlColumn.setFillWidth(true);
        ColumnConstraints previewColumn = new ColumnConstraints();
        previewColumn.setMinWidth(40);
        grid.getColumnConstraints().addAll(labelColumn, controlColumn, previewColumn);
        return grid;
    }

    private void addRowWithPreview(GridPane grid, int row, String labelText, Node control, Node preview) {
        Label label = new Label(labelText);
        label.setWrapText(true);
        grid.add(label, 0, row);
        if (control instanceof Region region && !(control instanceof Slider)) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        GridPane.setHgrow(control, Priority.ALWAYS);
        grid.add(control, 1, row);
        if (preview != null) {
            grid.add(preview, 2, row);
        }
    }

    private void addLayoutSliderRow(GridPane grid, int row, String labelText, Slider slider, Label valueLabel, Node preview) {
        Label label = new Label(labelText);
        label.setWrapText(true);
        grid.add(label, 0, row);
        HBox sliderBox = createSliderBox(slider, valueLabel);
        grid.add(sliderBox, 1, row);
        if (preview != null) {
            grid.add(preview, 2, row);
        }
    }

    private HBox createSliderBox(Slider slider, Label valueLabel) {
        HBox box = new HBox(8, slider, valueLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(260);
        box.setMaxWidth(260);
        valueLabel.setMinWidth(64);
        GridPane.setHgrow(box, Priority.NEVER);
        GridPane.setFillWidth(box, false);
        HBox.setHgrow(slider, Priority.NEVER);
        return box;
    }

    private Region createPreviewBox() {
        Region region = new Region();
        region.setPrefSize(30, 20);
        region.setMinSize(30, 20);
        region.setStyle("-fx-border-color: #888; -fx-border-width: 1;");
        return region;
    }

    private BarChart<String, Number> createBarChartPreview() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("BarChart");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(180);
        chart.setMinHeight(160);
        chart.setMaxWidth(Double.MAX_VALUE);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("A", 12));
        series.getData().add(new XYChart.Data<>("B", 18));
        series.getData().add(new XYChart.Data<>("C", 9));
        series.getData().add(new XYChart.Data<>("D", 15));
        series.getData().add(new XYChart.Data<>("E", 11));
        series.getData().add(new XYChart.Data<>("F", 7));
        series.getData().add(new XYChart.Data<>("G", 16));
        series.getData().add(new XYChart.Data<>("H", 13));
        chart.getData().add(series);
        return chart;
    }

    private PieChart createPieChartPreview() {
        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("A", 18),
                new PieChart.Data("B", 15),
                new PieChart.Data("C", 13),
                new PieChart.Data("D", 12),
                new PieChart.Data("E", 11),
                new PieChart.Data("F", 10),
                new PieChart.Data("G", 9),
                new PieChart.Data("H", 8)
        ));
        chart.setTitle("PieChart");
        chart.setAnimated(false);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(190);
        chart.setMinHeight(170);
        chart.setMaxWidth(Double.MAX_VALUE);
        return chart;
    }

    private void applyChartPreviewStyle(
            BarChart<String, Number> barChart,
            PieChart pieChart,
            String background,
            String text,
            String border,
            String chart1,
            String chart2,
            String chart3,
            String chart4,
            String chart5,
            String chart6,
            String chart7,
            String chart8
    ) {
        String chartStyle = String.format(
                "-fx-background-color: transparent; -fx-text-fill: %s; -fx-chart-color-1: %s; -fx-chart-color-2: %s; -fx-chart-color-3: %s; -fx-chart-color-4: %s; -fx-chart-color-5: %s; -fx-chart-color-6: %s; -fx-chart-color-7: %s; -fx-chart-color-8: %s; CHART_COLOR_1: %s; CHART_COLOR_2: %s; CHART_COLOR_3: %s; CHART_COLOR_4: %s; CHART_COLOR_5: %s; CHART_COLOR_6: %s; CHART_COLOR_7: %s; CHART_COLOR_8: %s;",
                text, chart1, chart2, chart3, chart4, chart5, chart6, chart7, chart8,
                chart1, chart2, chart3, chart4, chart5, chart6, chart7, chart8
        );
        String[] chartColors = {chart1, chart2, chart3, chart4, chart5, chart6, chart7, chart8};
        barChart.setStyle(chartStyle);
        pieChart.setStyle(chartStyle);

        if (!barChart.getData().isEmpty()) {
            int index = 0;
            for (XYChart.Data<String, Number> data : barChart.getData().get(0).getData()) {
                styleChartNode(data, "-fx-bar-fill: " + chartColors[index % chartColors.length] + ";");
                index++;
            }
        }

        int index = 0;
        for (PieChart.Data data : pieChart.getData()) {
            stylePieNode(data, "-fx-pie-color: " + chartColors[index % chartColors.length] + ";");
            index++;
        }
    }

    private void styleChartNode(XYChart.Data<String, Number> data, String style) {
        if (data.getNode() != null) {
            data.getNode().setStyle(style);
        } else {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle(style);
                }
            });
        }
    }

    private void stylePieNode(PieChart.Data data, String style) {
        if (data.getNode() != null) {
            data.getNode().setStyle(style);
        } else {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle(style);
                }
            });
        }
    }

    private void setVisibleManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void stylePreviewToggle(
            ToggleButton toggle,
            boolean selected,
            String accent,
            String cardBackground,
            String mutedText,
            String border
    ) {
        String background = selected ? accent : cardBackground;
        String textFill = selected ? "#ffffff" : mutedText;
        toggle.setStyle("-fx-background-color: " + background
                + "; -fx-text-fill: " + textFill
                + "; -fx-border-color: " + (selected ? accent : border)
                + "; -fx-background-radius: 999; -fx-border-radius: 999; -fx-padding: 6 18; -fx-font-weight: 600;");
    }

    private void enableWindowDrag(Node node) {
        final double[] dragOffset = new double[2];
        node.setOnMousePressed(event -> {
            dragOffset[0] = event.getSceneX();
            dragOffset[1] = event.getSceneY();
        });
        node.setOnMouseDragged(event -> {
            Window window = node.getScene() == null ? null : node.getScene().getWindow();
            if (window != null) {
                window.setX(event.getScreenX() - dragOffset[0]);
                window.setY(event.getScreenY() - dragOffset[1]);
            }
        });
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        return label;
    }

    private void validateColors(Color bg, Color text, Label validationLabel) {
        StringBuilder warnings = new StringBuilder();
        if (bg.getOpacity() < 0.3) {
            warnings.append("Arrière-plan trop transparent\n");
        }
        if (text.getOpacity() < 0.5) {
            warnings.append("Texte trop transparent\n");
        }
        double contrast = Math.abs(getBrightness(bg) - getBrightness(text));
        if (contrast < 0.3) {
            warnings.append("Faible contraste texte/fond\n");
        }
        validationLabel.setText(warnings.toString());
    }

    private double getBrightness(Color color) {
        return (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114) * color.getOpacity();
    }

    private String toHex(Color color) {
        return String.format(
                "#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }

    public record ThemeDraft(String name, String author, ThemeColors colors) {
    }
}
