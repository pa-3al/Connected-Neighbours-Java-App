package com.app.infrastructure.ui;

import com.app.domain.model.Neighbourhood;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.adapter.persistence.JdbcNeighbourhoodRepository;
import com.app.infrastructure.adapter.persistence.JdbcAddressRepository;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.Map;

public class DashboardNeighbourhoodController {

    @FXML private VBox rootNode;

    @FXML private BarChart<String, Number> populationChart;
    @FXML private CategoryAxis xAxisPopulation;
    @FXML private NumberAxis yAxisPopulation;
    @FXML private Label noDataLabelPop;

    @FXML private PieChart countryPieChart;
    @FXML private Label noDataLabelCountry;

    @FXML private BarChart<String, Number> usersPerNeighbourhoodChart;
    @FXML private CategoryAxis xAxisUsers;
    @FXML private NumberAxis yAxisUsers;
    @FXML private Label noDataLabelUsers;

    public DashboardNeighbourhoodController() {}

    @FXML
    public void initialize() {
        applyDynamicTheme();
        refresh();
        AppState.getInstance().syncingProperty().addListener((observable, wasSyncing, isSyncing) -> {
            if (wasSyncing && !isSyncing) {
                refresh();
            }
        });
    }

    public void refresh() {
        applyDynamicTheme();
        Platform.runLater(this::loadChartData);
    }

    // -------------------------------------------------------------------------
    // Thème
    // -------------------------------------------------------------------------

    private void applyDynamicTheme() {
        File cssFile = new File("src/main/resources/themes/dynamic.css");
        if (cssFile.exists() && rootNode != null) {
            rootNode.getStylesheets().clear();
            rootNode.getStylesheets().add(cssFile.toURI().toString());
        }
    }

    // -------------------------------------------------------------------------
    // Chargement des données
    // -------------------------------------------------------------------------

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
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Population");

            long maxPop = 0;

            for (Neighbourhood n : neighbourhoods) {
                if (n.name() != null && n.estimatedPopulation() != null) {
                    int population = Math.max(0, n.estimatedPopulation());
                    series.getData().add(new XYChart.Data<>(n.name(), population));
                    if (population > maxPop) maxPop = population;
                }
            }

            if (!series.getData().isEmpty()) {
                configureNumberAxis(yAxisPopulation, maxPop);
                populationChart.getData().clear();
                populationChart.getData().add(series);
                applyBarThemeClasses(series);
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
                    if (entry.getValue() != null && entry.getValue() > 0) {
                        String country = entry.getKey() == null || entry.getKey().isBlank()
                                ? "Inconnu" : entry.getKey();
                        countryPieChart.getData().add(new PieChart.Data(country, entry.getValue()));
                    }
                }
                if (countryPieChart.getData().isEmpty()) {
                    displayNoDataCountry();
                    return;
                }
                applyPieThemeClasses();
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
                    if (entry.getValue() != null) {
                        String neighbourhood = entry.getKey() == null || entry.getKey().isBlank()
                                ? "Inconnu" : entry.getKey();
                        int userCount = Math.max(0, entry.getValue());
                        series.getData().add(new XYChart.Data<>(neighbourhood, userCount));
                        if (userCount > maxUsers) maxUsers = userCount;
                    }
                }

                if (series.getData().isEmpty()) {
                    displayNoDataUsers();
                    return;
                }

                configureNumberAxis(yAxisUsers, maxUsers);
                usersPerNeighbourhoodChart.getData().clear();
                usersPerNeighbourhoodChart.getData().add(series);
                applyBarThemeClasses(series);
                usersPerNeighbourhoodChart.setVisible(true);
                noDataLabelUsers.setVisible(false);
            } else {
                displayNoDataUsers();
            }
        } catch (Exception e) {
            displayNoDataUsers();
        }
    }

    // -------------------------------------------------------------------------
    // Application des classes CSS du thème dynamique
    // -------------------------------------------------------------------------

    /**
     * Applique les classes CSS du thème sur les barres d'un BarChart.
     *
     * Les nœuds visuels des données ne sont créés par JavaFX qu'après le
     * premier layout pass. On utilise un listener sur nodeProperty() pour
     * s'assurer que la classe est bien appliquée dès que le nœud existe,
     * même si ce n'est pas encore le cas au moment de l'appel.
     */
    private void applyBarThemeClasses(XYChart.Series<String, Number> series) {
        int[] index = {0};
        for (XYChart.Data<String, Number> data : series.getData()) {
            final int i = index[0];
            if (data.getNode() != null) {
                styleBarNode(data.getNode(), i);
            } else {
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) styleBarNode(newNode, i);
                });
            }
            index[0]++;
        }
    }

    private void styleBarNode(Node bar, int index) {
        bar.getStyleClass().removeIf(s -> s.startsWith("default-color"));
        bar.getStyleClass().add("default-color" + (index % 8));
    }

    /**
     * Applique les classes CSS du thème sur les tranches d'un PieChart.
     * Même logique que pour les barres : listener sur nodeProperty().
     */
    private void applyPieThemeClasses() {
        int[] index = {0};
        for (PieChart.Data data : countryPieChart.getData()) {
            final int i = index[0];
            if (data.getNode() != null) {
                stylePieNode(data.getNode(), i);
            } else {
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) stylePieNode(newNode, i);
                });
            }
            index[0]++;
        }
    }

    private void stylePieNode(Node slice, int index) {
        slice.getStyleClass().removeIf(s -> s.startsWith("default-color"));
        slice.getStyleClass().add("default-color" + (index % 8));
    }

    // -------------------------------------------------------------------------
    // Affichage "pas de données"
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Utilitaires axe Y
    // -------------------------------------------------------------------------

    private void configureNumberAxis(NumberAxis axis, double maxValue) {
        if (axis == null) return;
        double upperBound = calculateUpperBound(maxValue);
        axis.setAutoRanging(false);
        axis.setLowerBound(0);
        axis.setUpperBound(upperBound);
        axis.setTickUnit(Math.max(1, upperBound / 5));
        axis.setMinorTickVisible(false);
    }

    private double calculateUpperBound(double maxValue) {
        if (maxValue <= 0) return 1;
        double padded = maxValue * 1.2;
        double magnitude = Math.pow(10, Math.floor(Math.log10(padded)));
        double normalized = padded / magnitude;

        if (normalized <= 2) return 2 * magnitude;
        if (normalized <= 5) return 5 * magnitude;
        return 10 * magnitude;
    }
}