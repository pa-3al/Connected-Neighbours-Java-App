package com.app.infrastructure.ui;

import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.adapter.persistence.JdbcServiceRepository;
import com.app.infrastructure.i18n.I18nService;
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
import java.util.Map;
import java.util.TreeMap;

public class DashboardServiceController {

    @FXML private VBox rootNode;

    @FXML private PieChart serviceStatusPieChart;
    @FXML private Label noDataLabelStatus;

    @FXML private BarChart<String, Number> expectedDatesBarChart;
    @FXML private CategoryAxis xAxisDates;
    @FXML private NumberAxis yAxisDates;
    @FXML private Label noDataLabelDates;

    public DashboardServiceController() {}

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

    private void applyDynamicTheme() {
        File cssFile = new File("src/main/resources/themes/dynamic.css");
        if (cssFile.exists() && rootNode != null) {
            if (!rootNode.getStyleClass().contains("root")) {
                rootNode.getStyleClass().add("root");
            }
            rootNode.getStylesheets().clear();
            rootNode.getStylesheets().add(cssFile.toURI().toString());
        }
    }

    private void loadChartData() {
        try {
            DatabaseConfig databaseConfig = new DatabaseConfig();
            JdbcServiceRepository serviceRepo = new JdbcServiceRepository(databaseConfig);

            loadStatusPieChart(serviceRepo);
            loadExpectedDatesBarChart(serviceRepo);

        } catch (Throwable t) {
            displayNoDataStatus();
            displayNoDataDates();
        }
    }

    private void loadStatusPieChart(JdbcServiceRepository repository) {
        if (serviceStatusPieChart == null) return;
        try {
            Map<String, Integer> statusData = repository.countServicesByStatus();
            if (!statusData.isEmpty()) {
                serviceStatusPieChart.getData().clear();
                for (Map.Entry<String, Integer> entry : statusData.entrySet()) {
                    if (entry.getValue() != null && entry.getValue() > 0) {
                        String rawStatus = entry.getKey();
                        String statusLabel = "Inconnu";

                        if (rawStatus != null && !rawStatus.isBlank()) {
                            String translated = I18nService.getInstance().get("service.status." + rawStatus);
                            statusLabel = translated.startsWith("!") ? rawStatus : translated;
                        }

                        serviceStatusPieChart.getData().add(new PieChart.Data(statusLabel, entry.getValue()));
                    }
                }
                if (serviceStatusPieChart.getData().isEmpty()) {
                    displayNoDataStatus();
                    return;
                }
                applyPieThemeClasses();
                serviceStatusPieChart.setVisible(true);
                noDataLabelStatus.setVisible(false);
            } else {
                displayNoDataStatus();
            }
        } catch (Exception e) {
            displayNoDataStatus();
        }
    }

    private void loadExpectedDatesBarChart(JdbcServiceRepository repository) {
        if (expectedDatesBarChart == null) return;
        try {
            Map<String, Integer> rawData = repository.countExpectedDatesByMonth();
            Map<String, Integer> sortedData = new TreeMap<>(rawData);

            if (!sortedData.isEmpty()) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Interventions");

                int maxCount = 0;

                for (Map.Entry<String, Integer> entry : sortedData.entrySet()) {
                    if (entry.getValue() != null) {
                        String monthKey = entry.getKey();
                        String displayMonth = monthKey;

                        if (monthKey != null && monthKey.matches("\\d{4}-\\d{2}")) {
                            String[] parts = monthKey.split("-");
                            displayMonth = parts[1] + "/" + parts[0];
                        }

                        int count = Math.max(0, entry.getValue());
                        series.getData().add(new XYChart.Data<>(displayMonth, count));
                        if (count > maxCount) maxCount = count;
                    }
                }

                if (series.getData().isEmpty()) {
                    displayNoDataDates();
                    return;
                }

                configureNumberAxis(yAxisDates, maxCount);
                expectedDatesBarChart.getData().clear();
                expectedDatesBarChart.getData().add(series);
                applyBarThemeClasses(series);
                expectedDatesBarChart.setVisible(true);
                noDataLabelDates.setVisible(false);
            } else {
                displayNoDataDates();
            }
        } catch (Exception e) {
            displayNoDataDates();
        }
    }

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

    private void applyPieThemeClasses() {
        int[] index = {0};
        for (PieChart.Data data : serviceStatusPieChart.getData()) {
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

    private void displayNoDataStatus() {
        if (serviceStatusPieChart != null) serviceStatusPieChart.setVisible(false);
        if (noDataLabelStatus != null) noDataLabelStatus.setVisible(true);
    }

    private void displayNoDataDates() {
        if (expectedDatesBarChart != null) expectedDatesBarChart.setVisible(false);
        if (noDataLabelDates != null) noDataLabelDates.setVisible(true);
    }

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