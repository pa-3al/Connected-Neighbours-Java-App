package com.app.infrastructure.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentStatus;
import com.app.domain.service.IncidentService;
import com.app.domain.service.UserService;
import com.app.infrastructure.i18n.I18nService;
import com.app.infrastructure.sync.IncidentSyncManager;
import com.app.infrastructure.sync.IncidentSyncReport;
import com.app.infrastructure.util.KeyboardShortcutsHandler;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;

public class IncidentController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final IncidentService incidentService;
    private final UserService userService;
    private final IncidentSyncManager syncManager;
    private final ConflictResolutionDialog conflictDialog = new ConflictResolutionDialog();
    private final I18nService i18n = I18nService.getInstance();

    @FXML private BorderPane incidentRoot;
    @FXML private TableView<Incident> incidentTable;
    @FXML private TableColumn<Incident, String> colId;
    @FXML private TableColumn<Incident, String> colTitle;
    @FXML private TableColumn<Incident, String> colCategory;
    @FXML private TableColumn<Incident, String> colStatus;
    @FXML private TableColumn<Incident, String> colSyncStatus;
    @FXML private TableColumn<Incident, Void> colDescription;
    @FXML private TableColumn<Incident, String> colReportedBy;
    @FXML private TableColumn<Incident, String> colDate;

    @FXML private Button btnResolve;
    @FXML private Button btnDelete;
    @FXML private Button btnSync;

    public IncidentController(IncidentService incidentService, UserService userService) {
        this.incidentService = incidentService;
        this.userService = userService;
        this.syncManager = new IncidentSyncManager(incidentService);
    }

    @FXML
    public void initialize() {
        setupColumns();
        loadData();

        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean selected = newSel != null;
            boolean isCompleted = newSel != null && newSel.status() == IncidentStatus.COMPLETED;

            btnResolve.setDisable(!selected || isCompleted);
            btnDelete.setDisable(!selected || isCompleted);
        });

        btnResolve.setDisable(true);
        btnDelete.setDisable(true);

        KeyboardShortcutsHandler.registerContext(incidentRoot, this::handleShortcut);
    }

    private boolean handleShortcut(KeyboardShortcutsHandler.ShortcutAction action, Node focusOwner) {
        if (focusOwner == null) {
            return false;
        }
        return switch (action) {
            case NEW_ITEM -> {
                handleAdd();
                yield true;
            }
            case REFRESH -> {
                handleRefresh();
                yield true;
            }
            case DELETE_ITEM -> {
                if (incidentTable.getSelectionModel().getSelectedItem() == null) {
                    yield false;
                }
                handleDelete();
                yield true;
            }
            case ESCAPE -> {
                if (incidentTable.getSelectionModel().getSelectedItem() == null) {
                    yield false;
                }
                incidentTable.getSelectionModel().clearSelection();
                yield true;
            }
            default -> false;
        };
    }

    private void setupColumns() {
        colId.setCellValueFactory(cd -> new SimpleStringProperty(shortId(cd.getValue().id())));
        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().title()));
        colCategory.setCellValueFactory(cd -> new SimpleStringProperty(translateEnum("incident.category.", cd.getValue().category())));
        colSyncStatus.setCellValueFactory(cd -> new SimpleStringProperty(translateEnum("sync.status.", cd.getValue().syncStatus())));

        colStatus.setCellValueFactory(cd -> {
            if (cd.getValue().status() != null) {
                String key = "incident.status." + cd.getValue().status().name().toLowerCase();
                return new SimpleStringProperty(i18n.get(key));
            }
            return new SimpleStringProperty("");
        });

        colReportedBy.setCellValueFactory(cd -> {
            String userId = cd.getValue().reportedByUserId();
            if (userId != null && !userId.isEmpty()) {
                String fullName = userService.getUserFullName(userId);
                return new SimpleStringProperty(fullName != null ? fullName : userId);
            }
            return new SimpleStringProperty(valOrEmpty(cd.getValue().reportedBy()));
        });

        colDate.setCellValueFactory(cd -> {
            if (cd.getValue().reportedAt() != null) {
                return new SimpleStringProperty(cd.getValue().reportedAt().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
        });

        colDescription.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button(i18n.get("incident.action.view"));
            {
                btn.setOnAction(event -> {
                    Incident incident = getTableView().getItems().get(getIndex());
                    String html = incident.description();
                    if (incident.adminResponseMessage() != null && !incident.adminResponseMessage().isBlank()) {
                        html += "<br><hr><br><b>" + i18n.get("incident.description.admin_response") + "</b><br>" + incident.adminResponseMessage();
                    }
                    showHtmlDialog(incident.title(), html);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    private void loadData() {
        incidentTable.getItems().setAll(incidentService.getAllIncidents());
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleSync() {
        try {
            userService.syncUsers();
            IncidentSyncReport report = syncManager.syncWithBackend(conflictDialog::resolve);
            loadData();
            showInfo(
                    i18n.get("incident.sync.report.title"),
                    i18n.get("incident.sync.report.body",
                            report.pushedToServer(), report.pulledFromServer(),
                            report.conflictsResolved(), report.conflictsUnresolved(), report.unchanged())
            );
        } catch (Exception e) {
            showError(i18n.get("incident.sync.error.title"), e.getMessage());
        }
    }

    @FXML
    private void handleResolve() {
        Incident selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(i18n.get("incident.resolve.title"));
        dialog.setHeaderText(i18n.get("incident.resolve.header", selected.title()));
        dialog.setContentText(i18n.get("incident.resolve.response.prompt"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                Incident resolved = selected.withStatus(IncidentStatus.COMPLETED);
                Incident updated = new Incident(
                        resolved.id(), resolved.title(), resolved.description(),
                        resolved.category(), resolved.status(), resolved.reportedByUserId(),
                        resolved.reportedBy(), result.get(), resolved.reportedAt(),
                        resolved.resolvedAt(), LocalDateTime.now(), resolved.syncStatus()
                );
                incidentService.updateIncident(updated);
                loadData();
            } catch (Exception e) {
                showError(i18n.get("incident.resolve.error"), e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Incident selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(i18n.get("action.delete"));
        alert.setHeaderText(null);
        alert.setContentText(i18n.get("confirm.delete"));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                incidentService.deleteIncident(selected.id());
                loadData();
            } catch (Exception e) {
                showError(i18n.get("incident.delete.error"), e.getMessage());
            }
        }
    }

    @FXML
    private void handleAdd() {
        Dialog<Incident> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("incident.create.title"));
        dialog.setHeaderText(i18n.get("incident.create.header"));

        ButtonType createBtn = new ButtonType(i18n.get("incident.create.button"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField title = new TextField();
        title.setPromptText(i18n.get("incident.title"));
        TextArea description = new TextArea();
        description.setPromptText(i18n.get("incident.description.prompt"));
        description.setPrefRowCount(3);
        ComboBox<IncidentCategory> category = new ComboBox<>();
        category.getItems().setAll(IncidentCategory.values());
        category.setConverter(new StringConverter<>() {
            @Override
            public String toString(IncidentCategory value) {
                return translateEnum("incident.category.", value);
            }

            @Override
            public IncidentCategory fromString(String string) {
                return null;
            }
        });

        grid.add(new Label(i18n.get("incident.create.label.title")), 0, 0);       grid.add(title, 1, 0);
        grid.add(new Label(i18n.get("incident.create.label.category")), 0, 1);     grid.add(category, 1, 1);
        grid.add(new Label(i18n.get("incident.create.label.description")), 0, 2);  grid.add(description, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == createBtn) {
                return Incident.create(
                        UUID.randomUUID().toString(), title.getText(), description.getText(),
                        category.getValue(), null
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(incident -> {
            incidentService.createIncident(incident);
            loadData();
        });
    }

    private void showHtmlDialog(String title, String htmlContent) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(i18n.get("incident.description.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        WebView webView = new WebView();
        webView.getEngine().loadContent(htmlContent != null ? htmlContent : "");
        webView.setPrefSize(600, 400);

        dialog.getDialogPane().setContent(webView);
        dialog.showAndWait();
    }

    private String shortId(String id) {
        if (id == null || id.length() <= 8) return valOrEmpty(id);
        return id.substring(0, 8) + "...";
    }

    private String valOrEmpty(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private String translateEnum(String prefix, Enum<?> value) {
        return value == null ? "" : i18n.get(prefix + value.name().toLowerCase());
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}