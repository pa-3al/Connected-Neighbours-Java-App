package com.app.infrastructure.ui;

import com.app.domain.model.Incident;
import com.app.domain.model.Incident.IncidentCategory;
import com.app.domain.model.Incident.IncidentPriority;
import com.app.domain.service.IncidentService;
import com.app.infrastructure.i18n.I18nService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

public class IncidentController {

    private final IncidentService incidentService;
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

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
        
        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean isSelected = newSelection != null;
            btnResolve.setDisable(!isSelected);
            btnDelete.setDisable(!isSelected);
        });
        
        btnResolve.setDisable(true);
        btnDelete.setDisable(true);
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().id().substring(0, 8) + "..."));
        colTitle.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().title()));
        colCategory.setCellValueFactory(cellData -> new SimpleStringProperty(valOrEmpty(cellData.getValue().category())));
        colPriority.setCellValueFactory(cellData -> new SimpleStringProperty(valOrEmpty(cellData.getValue().priority())));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(valOrEmpty(cellData.getValue().status())));
        colReportedBy.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().reportedBy()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().reportedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().reportedAt().format(formatter));
            }
            return new SimpleStringProperty("");
        });
    }
    
    private String valOrEmpty(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private void loadData() {
        incidentTable.getItems().setAll(incidentService.getAllIncidents());
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleResolve() {
        Incident selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                incidentService.resolveIncident(selected.id());
                loadData();
            } catch (Exception e) {
                showError("Error resolving incident", e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Incident selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
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
    }

    @FXML
    private void handleAdd() {
        Dialog<Incident> dialog = new Dialog<>();
        dialog.setTitle("New Incident");
        dialog.setHeaderText("Report a new incident");

        ButtonType loginButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

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

        grid.add(new Label("Title:"), 0, 0);
        grid.add(title, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(category, 1, 1);
        grid.add(new Label("Priority:"), 0, 2);
        grid.add(priority, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(location, 1, 3);
        grid.add(new Label("Reported By:"), 0, 4);
        grid.add(reporter, 1, 4);
        grid.add(new Label("Description:"), 0, 5);
        grid.add(description, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return Incident.create(
                    UUID.randomUUID().toString(),
                    title.getText(),
                    description.getText(),
                    category.getValue(),
                    priority.getValue(),
                    reporter.getText(),
                    location.getText()
                );
            }
            return null;
        });

        Optional<Incident> result = dialog.showAndWait();

        result.ifPresent(incident -> {
            incidentService.createIncident(incident);
            loadData();
        });
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
