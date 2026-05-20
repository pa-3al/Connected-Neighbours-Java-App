package com.app.infrastructure.ui.theme;

import java.util.Optional;

import com.app.infrastructure.i18n.I18nService;

import javafx.geometry.Insets;
import javafx.scene.Node;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
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
        dialog.setTitle(isEdit ? i18n.get("theme.dialog.edit.title") : i18n.get("theme.dialog.create.title"));
        dialog.setHeaderText(i18n.get("theme.dialog.header"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setResizable(true);

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

        Region bgPreview = createPreviewBox();
        Region cardPreview = createPreviewBox();
        Label textPreview = new Label(i18n.get("theme.preview.sample.text"));
        textPreview.setStyle("-fx-font-size:14px;");
        Label mutedPreview = new Label(i18n.get("theme.preview.sample.muted"));
        mutedPreview.setStyle("-fx-font-size:12px;");

        Region accentPreview = createPreviewBox();
        Region accentHoverPreview = createPreviewBox();

        Button btnPrimaryPreview = new Button(i18n.get("theme.editor.preview.primary"));
        Button btnSuccessPreview = new Button(i18n.get("theme.editor.preview.success"));
        Button btnWarningPreview = new Button(i18n.get("theme.editor.preview.warning"));
        Button btnDangerPreview = new Button(i18n.get("theme.editor.preview.danger"));

        TextField inputPreview = new TextField(i18n.get("theme.editor.preview.input"));
        inputPreview.setPrefWidth(80);
        Region inputBorderPreview = createPreviewBox();

        Region borderPreview = createPreviewBox();
        Region scrollPreview = createPreviewBox();

        Slider radiusSlider = new Slider(0, 30, initialColors.borderRadius());
        radiusSlider.setShowTickLabels(true);
        radiusSlider.setShowTickMarks(true);

        Slider sidebarWidthSlider = new Slider(150, 400, initialColors.sidebarWidth());
        sidebarWidthSlider.setShowTickLabels(true);
        sidebarWidthSlider.setShowTickMarks(true);

        ColorPicker sidebarColorPicker = new ColorPicker(
                initialColors.sidebarColor() != null ? initialColors.sidebarColor() : Color.web("#2b2b2b")
        );

        Button radiusPreview = new Button(i18n.get("theme.editor.preview.radius"));
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
        addRowWithPreview(baseGrid, row++, i18n.get("theme.editor.color.background"), bgColor, bgPreview);
        addRowWithPreview(baseGrid, row++, i18n.get("theme.editor.color.cards"), cardColor, cardPreview);
        addRowWithPreview(baseGrid, row++, i18n.get("theme.editor.color.primary_text"), textColor, textPreview);
        addRowWithPreview(baseGrid, row++, i18n.get("theme.editor.color.secondary_text"), mutedTextColor, mutedPreview);
        Tab baseTab = new Tab(i18n.get("theme.editor.tab.base"), baseGrid);

        GridPane accentGrid = createColorGrid();
        row = 0;
        accentGrid.add(createSectionLabel(i18n.get("theme.preview.section.accent")), 0, row++, 3, 1);
        addRowWithPreview(accentGrid, row++, i18n.get("theme.editor.color.accent"), accentColor, accentPreview);
        addRowWithPreview(accentGrid, row++, i18n.get("theme.editor.color.accent_hover"), accentHoverColor, accentHoverPreview);

        accentGrid.add(new Separator(), 0, row++, 3, 1);
        accentGrid.add(createSectionLabel(i18n.get("theme.preview.section.buttons")), 0, row++, 3, 1);
        addRowWithPreview(accentGrid, row++, i18n.get("theme.editor.color.button.primary"), btnPrimaryColor, btnPrimaryPreview);
        addRowWithPreview(accentGrid, row++, i18n.get("theme.editor.color.button.success"), btnSuccessColor, btnSuccessPreview);
        addRowWithPreview(accentGrid, row++, i18n.get("theme.editor.color.button.warning"), btnWarningColor, btnWarningPreview);
        addRowWithPreview(accentGrid, row++, i18n.get("theme.editor.color.button.danger"), btnDangerColor, btnDangerPreview);
        Tab accentTab = new Tab(i18n.get("theme.editor.tab.accent"), accentGrid);

        GridPane inputGrid = createColorGrid();
        row = 0;
        inputGrid.add(createSectionLabel(i18n.get("theme.preview.section.inputs")), 0, row++, 3, 1);
        addRowWithPreview(inputGrid, row++, i18n.get("theme.editor.color.input.background"), inputBgColor, inputPreview);
        addRowWithPreview(inputGrid, row++, i18n.get("theme.editor.color.input.border"), inputBorderColor, inputBorderPreview);

        inputGrid.add(new Separator(), 0, row++, 3, 1);
        inputGrid.add(createSectionLabel(i18n.get("theme.preview.section.ui")), 0, row++, 3, 1);
        addRowWithPreview(inputGrid, row++, i18n.get("theme.editor.color.border"), borderColor, borderPreview);
        addRowWithPreview(inputGrid, row++, i18n.get("theme.editor.color.scrollbar"), scrollbarColor, scrollPreview);
        inputGrid.add(validationLabel, 0, row++, 3, 1);
        Tab inputTab = new Tab(i18n.get("theme.editor.tab.input_ui"), inputGrid);

        GridPane layoutGrid = createColorGrid();
        row = 0;
        layoutGrid.add(createSectionLabel(i18n.get("theme.preview.section.layout")), 0, row++, 3, 1);
        addRowWithPreview(layoutGrid, row++, i18n.get("theme.editor.layout.radius"), radiusSlider, radiusPreview);

        layoutGrid.add(new Label(i18n.get("theme.preview.layout.sidebar")), 0, row);
        layoutGrid.add(sidebarWidthSlider, 1, row);
        layoutGrid.add(sidebarPreview, 2, row++);

        addRowWithPreview(layoutGrid, row++, i18n.get("theme.editor.layout.sidebar_color"), sidebarColorPicker, null);
        Tab layoutTab = new Tab(i18n.get("theme.editor.tab.layout"), layoutGrid);

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
        Button btnPrimary = new Button(i18n.get("theme.preview.button.primary"));
        Button btnAccent = new Button(i18n.get("theme.preview.button.accent"));
        btnRow1.getChildren().addAll(btnPrimary, btnAccent);

        HBox btnRow2 = new HBox(5);
        Button btnSuccess = new Button(i18n.get("theme.preview.button.success"));
        Button btnWarning = new Button(i18n.get("theme.preview.button.warning"));
        Button btnDanger = new Button(i18n.get("theme.preview.button.danger"));
        btnRow2.getChildren().addAll(btnSuccess, btnWarning, btnDanger);
        buttonsBox.getChildren().addAll(btnRow1, btnRow2);

        Label inputLabel = new Label(i18n.get("theme.preview.input"));
        TextField previewInput = new TextField();
        previewInput.setPromptText(i18n.get("theme.preview.input.placeholder"));
        previewInput.setText(i18n.get("theme.preview.input.text"));

        Label textAreaLabel = new Label(i18n.get("theme.preview.textarea"));
        TextArea previewTextArea = new TextArea(i18n.get("theme.preview.textarea.content"));
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
            btnLabel.setStyle(labelStyle);
            inputLabel.setStyle(labelStyle);
            textAreaLabel.setStyle(labelStyle);
            progressLabel.setStyle(labelStyle);
            sepLabel.setStyle(labelStyle);
            badgeLabel.setStyle(labelStyle);
            listLabel.setStyle(labelStyle);
            scrollLabel.setStyle(labelStyle + " -fx-font-style: italic;");

            String btnBase = "-fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 10;";
            btnPrimary.setStyle("-fx-background-color: " + btnPrimaryHex + "; " + btnBase);
            btnAccent.setStyle("-fx-background-color: " + accentHex + "; " + btnBase);
            btnSuccess.setStyle("-fx-background-color: " + btnSuccessHex + "; " + btnBase);
            btnWarning.setStyle("-fx-background-color: " + btnWarningHex + "; " + btnBase);
            btnDanger.setStyle("-fx-background-color: " + btnDangerHex + "; " + btnBase);

            previewInput.setStyle("-fx-background-color: " + inputBgHex + "; -fx-text-fill: " + textHex
                    + "; -fx-border-color: " + inputBorderHex + "; -fx-background-radius: 5; -fx-border-radius: 5;"
                    + " -fx-prompt-text-fill: " + mutedHex + ";");
            previewTextArea.setStyle("-fx-control-inner-background: " + inputBgHex + "; -fx-text-fill: " + textHex + ";");
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

            radiusPreview.setStyle("-fx-background-color: " + btnPrimaryHex + "; -fx-text-fill: white; -fx-background-radius: " + radius + ";");
            sidebarPreview.setStyle("-fx-background-color: " + sidebarColorHex + ";");
            sidebarPreview.setPrefWidth(sidebarWidthSlider.getValue() * 0.5);
        };

        ColorPicker[] pickers = {
                bgColor, cardColor, textColor, mutedTextColor, accentColor, accentHoverColor,
                btnPrimaryColor, btnSuccessColor, btnWarningColor, btnDangerColor, inputBgColor, inputBorderColor,
                borderColor, scrollbarColor, sidebarColorPicker
        };

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
        ButtonType saveBtnType = new ButtonType(
                isEdit ? i18n.get("theme.dialog.save") : i18n.get("theme.dialog.create"),
                ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(700, 450);

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
            author = i18n.get("theme.author.default");
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
                sidebarColorPicker.getValue()
        );

        return Optional.of(new ThemeDraft(themeName, author, colors));
    }

    private GridPane createColorGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        return grid;
    }

    private void addRowWithPreview(GridPane grid, int row, String labelText, Node control, Node preview) {
        grid.add(new Label(labelText), 0, row);
        grid.add(control, 1, row);
        if (preview != null) {
            grid.add(preview, 2, row);
        }
    }

    private Region createPreviewBox() {
        Region region = new Region();
        region.setPrefSize(30, 20);
        region.setMinSize(30, 20);
        region.setStyle("-fx-border-color: #888; -fx-border-width: 1;");
        return region;
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        return label;
    }

    private void validateColors(Color bg, Color text, Label validationLabel) {
        StringBuilder warnings = new StringBuilder();
        if (bg.getOpacity() < 0.3) {
            warnings.append(i18n.get("theme.validation.background.opacity")).append("\n");
        }
        if (text.getOpacity() < 0.5) {
            warnings.append(i18n.get("theme.validation.text.opacity")).append("\n");
        }
        double contrast = Math.abs(getBrightness(bg) - getBrightness(text));
        if (contrast < 0.3) {
            warnings.append(i18n.get("theme.validation.contrast")).append("\n");
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
