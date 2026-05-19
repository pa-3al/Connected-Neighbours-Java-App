package com.app.infrastructure.ui;

import com.app.domain.model.Neighbourhood;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.adapter.persistence.JdbcNeighbourhoodRepository;
import com.app.infrastructure.adapter.persistence.JdbcAddressRepository;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Map;

public class DashboardNeighbourhoodController {

    @FXML
    private BarChart<String, Number> populationChart;
    @FXML
    private CategoryAxis xAxisPopulation;
    @FXML
    private NumberAxis yAxisPopulation;
    @FXML
    private Label noDataLabelPop;

    @FXML
    private PieChart countryPieChart;
    @FXML
    private Label noDataLabelCountry;

    @FXML
    private BarChart<String, Number> usersPerNeighbourhoodChart;
    @FXML
    private CategoryAxis xAxisUsers;
    @FXML
    private NumberAxis yAxisUsers;
    @FXML
    private Label noDataLabelUsers;

    public DashboardNeighbourhoodController() {}

    @FXML
    public void initialize() {
        Platform.runLater(this::loadChartData);
    }

    private void loadChartData() {
        try {
            DatabaseConfig databaseConfig = new DatabaseConfig();
            JdbcNeighbourhoodRepository neighbourhoodRepo = new JdbcNeighbourhoodRepository(databaseConfig);
            JdbcAddressRepository addressRepo = new JdbcAddressRepository(databaseConfig);

            loadPopulationChart(neighbourhoodRepo);
            loadCountryPieChart(addressRepo);
            loadUsersPerNeighbourhoodChart(addressRepo);

        } catch (Throwable t) {
            displayNoDataPop();
            displayNoDataCountry();
            displayNoDataUsers();
        }
    }

    private void loadPopulationChart(JdbcNeighbourhoodRepository repository) {
        if (populationChart == null) return;
        try {
            List<Neighbourhood> neighbourhoods = repository.findAll();
            long maxPop = neighbourhoods.stream()
                    .filter(n -> n.estimatedPopulation() != null)
                    .mapToLong(Neighbourhood::estimatedPopulation)
                    .max()
                    .orElse(0);

            if (maxPop > 0) {
                yAxisPopulation.setAutoRanging(false);
                yAxisPopulation.setLowerBound(0);
                double upperBound = Math.ceil(maxPop * 1.2 / 50000) * 50000;
                if (upperBound <= 0) upperBound = 1000;
                yAxisPopulation.setUpperBound(upperBound);
                double tickUnit = upperBound / 5;
                yAxisPopulation.setTickUnit(tickUnit > 0 ? tickUnit : 100);

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Population");

                for (Neighbourhood n : neighbourhoods) {
                    if (n.name() != null && n.estimatedPopulation() != null) {
                        series.getData().add(new XYChart.Data<>(n.name(), n.estimatedPopulation()));
                    }
                }

                populationChart.getData().clear();
                populationChart.getData().add(series);
                populationChart.setVisible(true);
                noDataLabelPop.setVisible(false);
            } else {
                displayNoDataPop();
            }
        } catch (Exception e) {
            displayNoDataPop();
        }
    }

    private void loadCountryPieChart(JdbcAddressRepository repository) {
        if (countryPieChart == null) return;
        try {
            Map<String, Integer> countryData = repository.countAddressesPerCountry();
            if (!countryData.isEmpty()) {
                countryPieChart.getData().clear();
                for (Map.Entry<String, Integer> entry : countryData.entrySet()) {
                    String country = entry.getKey() == null || entry.getKey().isEmpty() ? "Inconnu" : entry.getKey();
                    countryPieChart.getData().add(new PieChart.Data(country, entry.getValue()));
                }
                countryPieChart.setVisible(true);
                noDataLabelCountry.setVisible(false);
            } else {
                displayNoDataCountry();
            }
        } catch (Exception e) {
            displayNoDataCountry();
        }
    }

    private void loadUsersPerNeighbourhoodChart(JdbcAddressRepository repository) {
        if (usersPerNeighbourhoodChart == null) return;
        try {
            Map<String, Integer> usersData = repository.countUsersPerNeighbourhood();
            if (!usersData.isEmpty()) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Utilisateurs");

                int maxUsers = 0;

                for (Map.Entry<String, Integer> entry : usersData.entrySet()) {
                    series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    if (entry.getValue() > maxUsers) {
                        maxUsers = entry.getValue();
                    }
                }

                yAxisUsers.setAutoRanging(false);
                yAxisUsers.setLowerBound(0);
                double upperBound = Math.ceil(maxUsers * 1.2);
                if (upperBound <= 0) upperBound = 10;
                yAxisUsers.setUpperBound(upperBound);
                yAxisUsers.setTickUnit(Math.max(1, Math.ceil(upperBound / 5)));

                usersPerNeighbourhoodChart.getData().clear();
                usersPerNeighbourhoodChart.getData().add(series);
                usersPerNeighbourhoodChart.setVisible(true);
                noDataLabelUsers.setVisible(false);
            } else {
                displayNoDataUsers();
            }
        } catch (Exception e) {
            displayNoDataUsers();
        }
    }

    private void displayNoDataPop() {
        if (populationChart != null) populationChart.setVisible(false);
        if (noDataLabelPop != null) noDataLabelPop.setVisible(true);
    }

    private void displayNoDataCountry() {
        if (countryPieChart != null) countryPieChart.setVisible(false);
        if (noDataLabelCountry != null) noDataLabelCountry.setVisible(true);
    }

    private void displayNoDataUsers() {
        if (usersPerNeighbourhoodChart != null) usersPerNeighbourhoodChart.setVisible(false);
        if (noDataLabelUsers != null) noDataLabelUsers.setVisible(true);
    }
}