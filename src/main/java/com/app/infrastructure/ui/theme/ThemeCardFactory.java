package com.app.infrastructure.ui.theme;

import java.util.function.Consumer;

import com.app.domain.model.Theme;
import com.app.infrastructure.i18n.I18nService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class ThemeCardFactory {

    private final I18nService i18n;

    public ThemeCardFactory(I18nService i18n) {
        this.i18n = i18n;
    }

    public VBox create(
            Theme theme,
            Theme currentTheme,
            Consumer<String> onApply,
            Consumer<Theme> onEdit,
            Consumer<Theme> onExport,
            Consumer<Theme> onDelete
    ) {
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
        applyBtn.setText(isCurrent ? i18n.get("theme.preview.badge.active") : i18n.get("theme.apply"));
        applyBtn.getStyleClass().add(isCurrent ? "success-button" : "primary-button");
        applyBtn.setDisable(isCurrent);
        applyBtn.setOnAction(e -> onApply.accept(theme.id()));
        buttons.getChildren().add(applyBtn);

        if (!theme.isBuiltIn()) {
            Button editBtn = new Button("📝");
            editBtn.getStyleClass().add("primary-button");
            editBtn.setTooltip(new Tooltip(i18n.get("theme.edit.tooltip")));
            editBtn.setOnAction(e -> onEdit.accept(theme));

            Button exportBtn = new Button("📤");
            exportBtn.getStyleClass().add("secondary-button");
            exportBtn.setTooltip(new Tooltip(i18n.get("theme.export.tooltip")));
            exportBtn.setOnAction(e -> onExport.accept(theme));

            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().add("danger-button");
            deleteBtn.setTooltip(new Tooltip(i18n.get("theme.delete.tooltip")));
            deleteBtn.setOnAction(e -> onDelete.accept(theme));

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
}
