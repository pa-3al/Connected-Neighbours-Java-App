package com.app.infrastructure.ui;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentPriority;
import com.app.domain.service.IncidentService;
import com.app.infrastructure.i18n.I18nService;
import com.app.infrastructure.sync.IncidentSyncManager;
import com.app.infrastructure.sync.IncidentSyncReport;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class IncidentController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IncidentService incidentService;
    private final IncidentSyncManager syncManager;
    private final ConflictResolutionDialog conflictDialog = new ConflictResolutionDialog();
    private final I18nService i18n = I18nService.getInstance();

    @FXML private TableView<Incident> incidentTable;
    @FXML private TableColumn<Incident, String> colId;
    @FXML private TableColumn<Incident, String> colTitle;
    @FXML private TableColumn<Incident, String> colCategory;
    @FXML private TableColumn<Incident, String> colPriority;
    @FXML private TableColumn<Incident, String> colStatus;
    @FXML private TableColumn<Incident, String> colReportedBy;
    @FXML private TableColumn<Incident, String> colDate;

    @FXML private Button btnResolve;
    @FXML private Button btnDelete;
    @FXML private Button btnSync;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
        this.syncManager = new IncidentSyncManager(incidentService);
    }

    @FXML
    public void initialize() {
        setupColumns();
        loadData();

        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean selected = newSel != null;
            btnResolve.setDisable(!selected);
            btnDelete.setDisable(!selected);
        });

        btnResolve.setDisable(true);
        btnDelete.setDisable(true);
    }

    private void setupColumns() {
        colId.setCellValueFactory(cd -> new SimpleStringProperty(shortId(cd.getValue().id())));
        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().title()));
        colCategory.setCellValueFactory(cd -> new SimpleStringProperty(valOrEmpty(cd.getValue().category())));
        colPriority.setCellValueFactory(cd -> new SimpleStringProperty(valOrEmpty(cd.getValue().priority())));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(valOrEmpty(cd.getValue().status())));
        colReportedBy.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().reportedBy()));
        colDate.setCellValueFactory(cd -> {
            if (cd.getValue().reportedAt() != null) {
                return new SimpleStringProperty(cd.getValue().reportedAt().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
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
        try {
            incidentService.resolveIncident(selected.id());
            loadData();
        } catch (Exception e) {
            showError("Error resolving incident", e.getMessage());
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
                showError("Error deleting incident", e.getMessage());
            }
        }
    }

    @FXML
    private void handleAdd() {
        Dialog<Incident> dialog = new Dialog<>();
        dialog.setTitle("New Incident");
        dialog.setHeaderText("Report a new incident");

        ButtonType createBtn = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField title = new TextField();
        title.setPromptText("Title");
        TextArea description = new TextArea();
        description.setPromptText("Description");
        description.setPrefRowCount(3);
        ComboBox<IncidentCategory> category = new ComboBox<>();
        category.getItems().setAll(IncidentCategory.values());
        ComboBox<IncidentPriority> priority = new ComboBox<>();
        priority.getItems().setAll(IncidentPriority.values());
        TextField location = new TextField();
        location.setPromptText("Location");
        TextField reporter = new TextField();
        reporter.setPromptText("Reported By");

        grid.add(new Label("Title:"), 0, 0);       grid.add(title, 1, 0);
        grid.add(new Label("Category:"), 0, 1);     grid.add(category, 1, 1);
        grid.add(new Label("Priority:"), 0, 2);     grid.add(priority, 1, 2);
        grid.add(new Label("Location:"), 0, 3);     grid.add(location, 1, 3);
        grid.add(new Label("Reported By:"), 0, 4);  grid.add(reporter, 1, 4);
        grid.add(new Label("Description:"), 0, 5);  grid.add(description, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == createBtn) {
                return Incident.create(
                    UUID.randomUUID().toString(), title.getText(), description.getText(),
                    category.getValue(), priority.getValue(), reporter.getText(), location.getText()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(incident -> {
            incidentService.createIncident(incident);
            loadData();
        });
    }

    private String shortId(String id) {
        if (id == null || id.length() <= 8) return valOrEmpty(id);
        return id.substring(0, 8) + "...";
    }

    private String valOrEmpty(Object obj) {
        return obj == null ? "" : obj.toString();
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
