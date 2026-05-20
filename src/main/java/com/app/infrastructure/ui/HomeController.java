package com.app.infrastructure.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class HomeController {

    @FXML
    private TabPane dashboardTabPane;

    @FXML
    private Tab tabNeighbourhoods;

    @FXML
    private Tab tabEvents;

    @FXML
    private Tab tabServices;

    @FXML
    private DashboardNeighbourhoodController dashboardNeighbourhoodController;

    @FXML
    public void initialize() {
        tabNeighbourhoods.selectedProperty().addListener((observable, wasSelected, isSelected) -> {
            if (isSelected && dashboardNeighbourhoodController != null) {
                dashboardNeighbourhoodController.refresh();
            }
        });
    }
}
