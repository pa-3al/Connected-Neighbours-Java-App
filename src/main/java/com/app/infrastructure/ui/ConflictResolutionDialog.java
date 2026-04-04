package com.app.infrastructure.ui;

import com.app.domain.model.Incident;
import com.app.domain.model.SyncStatus;
import com.app.infrastructure.i18n.I18nService;
import com.app.infrastructure.sync.IncidentConflict;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConflictResolutionDialog {

    enum SourceChoice { LOCAL, SERVER }

    private static final DateTimeFormatter DETAIL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final I18nService i18n = I18nService.getInstance();

    public Incident resolve(IncidentConflict conflict) {
        Incident local = conflict.localIncident();
        Incident server = conflict.serverIncident();
        SourceChoice baseRec = preferredSide(local, server);

        Dialog<Incident> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("incident.sync.conflict.title"));
        dialog.setHeaderText(null);

        String css = Objects.requireNonNull(
                getClass().getResource("/com/app/view/incident_sync.css")
        ).toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);

        ButtonType resolveBtn = new ButtonType(i18n.get("incident.sync.conflict.apply"), ButtonBar.ButtonData.OK_DONE);
        ButtonType skipBtn = new ButtonType(i18n.get("incident.sync.conflict.skip"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(resolveBtn, skipBtn);

        Label warnIcon = new Label("\u26A0");
        warnIcon.getStyleClass().add("conflict-header-icon");
        Label headerLabel = new Label(i18n.get("incident.sync.conflict.header", shortId(local.id())));
        headerLabel.getStyleClass().add("conflict-header-label");
        HBox header = new HBox(10, warnIcon, headerLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 10, 16));
        header.getStyleClass().add("conflict-header");

        List<FieldResolver> allResolvers = new ArrayList<>();

        VBox blocks = new VBox(2);
        blocks.setPadding(new Insets(12));
        blocks.getStyleClass().add("conflict-blocks");

        FieldResolver titleRes = addMergeBlock(blocks, i18n.get("incident.sync.field.title"), local.title(), server.title(), baseRec, allResolvers);
        FieldResolver descRes = addMergeBlock(blocks, i18n.get("incident.sync.field.description"), local.description(), server.description(), baseRec, allResolvers);
        FieldResolver catRes = addMergeBlock(blocks, i18n.get("incident.sync.field.category"), local.category(), server.category(), baseRec, allResolvers);
        FieldResolver statusRes = addMergeBlock(blocks, i18n.get("incident.sync.field.status"), local.status(), server.status(), baseRec, allResolvers);
        FieldResolver adminMessageRes = addMergeBlock(blocks, i18n.get("incident.sync.field.admin_response"), local.adminResponseMessage(), server.adminResponseMessage(), baseRec, allResolvers);
        FieldResolver reportedAtRes = addMergeBlock(blocks, i18n.get("incident.sync.field.reported_at"), local.reportedAt(), server.reportedAt(), baseRec, allResolvers);
        FieldResolver resolvedAtRes = addMergeBlock(blocks, i18n.get("incident.sync.field.resolved_at"), local.resolvedAt(), server.resolvedAt(), baseRec, allResolvers);

        Button acceptAllLocal = new Button("\u2713 " + i18n.get("incident.sync.conflict.keep_local_all"));
        acceptAllLocal.getStyleClass().add("btn-accept-all-local");
        acceptAllLocal.setOnAction(e -> allResolvers.forEach(r -> r.select(SourceChoice.LOCAL)));

        Button acceptAllServer = new Button("\u2713 " + i18n.get("incident.sync.conflict.keep_server_all"));
        acceptAllServer.getStyleClass().add("btn-accept-all-server");
        acceptAllServer.setOnAction(e -> allResolvers.forEach(r -> r.select(SourceChoice.SERVER)));

        Region tbSpacer = new Region();
        HBox.setHgrow(tbSpacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, tbSpacer, acceptAllLocal, acceptAllServer);
        toolbar.setPadding(new Insets(8, 16, 8, 16));
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.getStyleClass().add("conflict-toolbar");

        ScrollPane scrollPane = new ScrollPane(blocks);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("conflict-scroll");

        VBox mainLayout = new VBox(0, header, toolbar, scrollPane);
        mainLayout.setPrefSize(920, 660);

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getStyleClass().add("conflict-dialog");
        dialog.setResizable(true);

        dialog.setOnShown(evt -> dialog.getDialogPane().lookupAll(".button").forEach(node -> {
            if (node instanceof Button b) {
                if (b.getText().equals(i18n.get("incident.sync.conflict.apply"))) {
                    b.getStyleClass().add("btn-resolve");
                } else {
                    b.getStyleClass().add("btn-skip");
                }
            }
        }));

        dialog.setResultConverter(buttonType -> {
            if (buttonType != resolveBtn) return null;
            return new Incident(
                    local.id(),
                    pickField(titleRes, local.title(), server.title()),
                    pickField(descRes, local.description(), server.description()),
                    pickField(catRes, local.category(), server.category()),
                    pickField(statusRes, local.status(), server.status()),
                    server.reportedByUserId() != null && !server.reportedByUserId().isBlank() ? server.reportedByUserId() : local.reportedByUserId(),
                    server.reportedBy() != null && !server.reportedBy().isBlank() ? server.reportedBy() : local.reportedBy(),
                    pickField(adminMessageRes, local.adminResponseMessage(), server.adminResponseMessage()),
                    pickField(reportedAtRes, local.reportedAt(), server.reportedAt()),
                    pickField(resolvedAtRes, local.resolvedAt(), server.resolvedAt()),
                    LocalDateTime.now(),
                    SyncStatus.SYNCED
            );
        });

        return dialog.showAndWait().orElse(null);
    }

    private FieldResolver addMergeBlock(VBox container, String fieldName, Object localValue,
                                        Object serverValue, SourceChoice baseRec, List<FieldResolver> allResolvers) {
        boolean isDifferent = !Objects.equals(localValue, serverValue);
        SourceChoice recommended = recommendedChoice(localValue, serverValue, baseRec);
        FieldResolver resolver = new FieldResolver(recommended);
        allResolvers.add(resolver);

        if (!isDifferent) {
            Label check = new Label("\u2713");
            check.getStyleClass().add("identical-check");
            Label name = new Label(fieldName);
            name.getStyleClass().add("identical-name");
            Label dash = new Label("\u2014");
            dash.getStyleClass().add("identical-dash");
            Label val = new Label(formatValue(localValue));
            val.getStyleClass().add("identical-value");
            val.setMaxWidth(600);
            val.setWrapText(true);
            HBox row = new HBox(8, check, name, dash, val);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 12, 5, 12));
            row.getStyleClass().add("identical-row");
            container.getChildren().add(row);
            return resolver;
        }

        VBox block = new VBox(0);
        block.getStyleClass().add("merge-block");

        Label fieldLabel = new Label(fieldName);
        fieldLabel.getStyleClass().add("merge-field-label");

        ToggleGroup group = new ToggleGroup();
        ToggleButton localBtn = new ToggleButton("Accept current");
        ToggleButton serverBtn = new ToggleButton("Accept incoming");
        localBtn.setToggleGroup(group);
        serverBtn.setToggleGroup(group);

        Region fldSpacer = new Region();
        HBox.setHgrow(fldSpacer, Priority.ALWAYS);
        HBox blockHeader = new HBox(8, fieldLabel, fldSpacer, localBtn, serverBtn);
        blockHeader.setAlignment(Pos.CENTER_LEFT);
        blockHeader.setPadding(new Insets(8, 12, 8, 12));
        blockHeader.getStyleClass().add("merge-block-header");

        Label localMarker = new Label("<<<<<<< " + i18n.get("incident.sync.choice.local") + " (current)");
        localMarker.getStyleClass().add("merge-marker-local");
        localMarker.setPadding(new Insets(8, 12, 2, 12));
        Label localValLabel = new Label(formatValue(localValue));
        localValLabel.setWrapText(true);
        localValLabel.setMaxWidth(Double.MAX_VALUE);
        localValLabel.getStyleClass().add("merge-value");
        localValLabel.setPadding(new Insets(4, 12, 8, 28));
        VBox localSection = new VBox(0, localMarker, localValLabel);
        localSection.getStyleClass().add("merge-section-local");

        Label separator = new Label("=======");
        separator.getStyleClass().add("merge-separator");
        separator.setPadding(new Insets(2, 12, 2, 12));

        Label serverMarker = new Label(">>>>>>> " + i18n.get("incident.sync.choice.server") + " (incoming)");
        serverMarker.getStyleClass().add("merge-marker-server");
        serverMarker.setPadding(new Insets(8, 12, 2, 12));
        Label serverValLabel = new Label(formatValue(serverValue));
        serverValLabel.setWrapText(true);
        serverValLabel.setMaxWidth(Double.MAX_VALUE);
        serverValLabel.getStyleClass().add("merge-value");
        serverValLabel.setPadding(new Insets(4, 12, 8, 28));
        VBox serverSection = new VBox(0, serverMarker, serverValLabel);
        serverSection.getStyleClass().add("merge-section-server");

        block.getChildren().addAll(blockHeader, localSection, separator, serverSection);

        resolver.bindControls(localBtn, serverBtn, group, localSection, serverSection);
        group.selectToggle(recommended == SourceChoice.LOCAL ? localBtn : serverBtn);

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) { group.selectToggle(oldToggle); return; }
            resolver.choice = newToggle == localBtn ? SourceChoice.LOCAL : SourceChoice.SERVER;
            resolver.updateVisuals();
        });

        VBox wrapper = new VBox(block);
        wrapper.setPadding(new Insets(4, 0, 4, 0));
        container.getChildren().add(wrapper);
        return resolver;
    }

    private String formatValue(Object value) {
        if (value == null) return i18n.get("incident.sync.value.empty");
        if (value instanceof LocalDateTime dateTime) return dateTime.format(DETAIL_DATE_FORMATTER);
        String text = value.toString();
        return text.isBlank() ? i18n.get("incident.sync.value.empty") : text;
    }

    private String shortId(String id) {
        if (id == null || id.length() <= 8) return id == null ? "" : id;
        return id.substring(0, 8) + "...";
    }

    private SourceChoice preferredSide(Incident local, Incident server) {
        LocalDateTime localMod = local.lastModified();
        LocalDateTime serverMod = server.lastModified();
        if (localMod == null && serverMod != null) return SourceChoice.SERVER;
        if (serverMod == null && localMod != null) return SourceChoice.LOCAL;
        if (localMod == null) return SourceChoice.LOCAL;
        if (serverMod == null) return SourceChoice.LOCAL;
        return serverMod.isAfter(localMod) ? SourceChoice.SERVER : SourceChoice.LOCAL;
    }

    private SourceChoice recommendedChoice(Object localValue, Object serverValue, SourceChoice fallback) {
        if (Objects.equals(localValue, serverValue)) return fallback;
        if (localValue == null && serverValue != null) return SourceChoice.SERVER;
        if (localValue != null && serverValue == null) return SourceChoice.LOCAL;
        return fallback;
    }

    private <T> T pickField(FieldResolver resolver, T localValue, T serverValue) {
        return resolver.getChoice() == SourceChoice.LOCAL ? localValue : serverValue;
    }

    static class FieldResolver {
        SourceChoice choice;
        private ToggleButton localBtn;
        private ToggleButton serverBtn;
        private ToggleGroup toggleGroup;
        private VBox localSection;
        private VBox serverSection;

        FieldResolver(SourceChoice initial) { this.choice = initial; }

        void bindControls(ToggleButton local, ToggleButton server, ToggleGroup group,
                          VBox localSec, VBox serverSec) {
            this.localBtn = local;
            this.serverBtn = server;
            this.toggleGroup = group;
            this.localSection = localSec;
            this.serverSection = serverSec;
            updateVisuals();
        }

        void select(SourceChoice c) {
            this.choice = c;
            if (toggleGroup != null) {
                toggleGroup.selectToggle(c == SourceChoice.LOCAL ? localBtn : serverBtn);
            }
            updateVisuals();
        }

        SourceChoice getChoice() { return choice; }

        void updateVisuals() {
            if (localSection != null) {
                boolean isLocal = choice == SourceChoice.LOCAL;
                localSection.setOpacity(isLocal ? 1.0 : 0.4);
                serverSection.setOpacity(isLocal ? 0.4 : 1.0);
            }
            if (localBtn != null) {
                boolean isLocal = choice == SourceChoice.LOCAL;
                applyToggleClass(localBtn, isLocal, true);
                applyToggleClass(serverBtn, !isLocal, false);
            }
        }

        private static void applyToggleClass(ToggleButton btn, boolean selected, boolean isLocal) {
            btn.getStyleClass().removeAll("toggle-btn-local-selected", "toggle-btn-server-selected", "toggle-btn-unselected");
            if (selected) {
                btn.getStyleClass().add(isLocal ? "toggle-btn-local-selected" : "toggle-btn-server-selected");
            } else {
                btn.getStyleClass().add("toggle-btn-unselected");
            }
        }
    }
}