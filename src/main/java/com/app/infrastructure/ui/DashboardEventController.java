package com.app.infrastructure.ui;

import com.app.domain.port.out.EventStatsRepository;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.adapter.persistence.JdbcEventStatsRepository;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DashboardEventController {

    @FXML private PieChart priceTypeChart;
    @FXML private Label noDataLabelPriceType;

    @FXML private BarChart<String, Number> priceDistributionChart;
    @FXML private CategoryAxis xAxisPriceDist;
    @FXML private NumberAxis yAxisPriceDist;
    @FXML private Label noDataLabelPriceDist;

    @FXML private LineChart<String, Number> avgPriceChart;
    @FXML private CategoryAxis xAxisAvgPrice;
    @FXML private NumberAxis yAxisAvgPrice;
    @FXML private Label noDataLabelAvgPrice;

    @FXML private BarChart<String, Number> topParticipationsChart;
    @FXML private NumberAxis yAxisTop;
    @FXML private Label noDataLabelTop;

    @FXML private LineChart<String, Number> eventsPerMonthChart;
    @FXML private Label noDataLabelMonth;

    private final EventStatsRepository repo;

    public DashboardEventController() {
        this.repo = new JdbcEventStatsRepository(new DatabaseConfig());
    }

    @FXML
    public void initialize() {
        refresh();
    }

    public void refresh() {
        new Thread(this::load).start();
    }

    private void load() {
        try {
            var priceType = repo.getEventsByPriceType();
            var priceDist = repo.getEventPriceDistribution();
            var avgPrice = repo.getAveragePriceByMonth();
            var top = repo.getTopEventsByParticipation();
            var month = repo.getEventsByMonth();

            Platform.runLater(() -> {
                updatePie(priceTypeChart, priceType, noDataLabelPriceType);
                updateBar(priceDistributionChart, priceDist, noDataLabelPriceDist);
                updateLineDouble(avgPriceChart, avgPrice, noDataLabelAvgPrice);
                updateBar(topParticipationsChart, top, noDataLabelTop);
                updateLineInt(eventsPerMonthChart, month, noDataLabelMonth);
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                noDataLabelPriceType.setVisible(true);
                noDataLabelPriceDist.setVisible(true);
                noDataLabelAvgPrice.setVisible(true);
                noDataLabelTop.setVisible(true);
                noDataLabelMonth.setVisible(true);
            });
        }
    }

    private void updatePie(PieChart chart, Map<String, Integer> data, Label label) {
        chart.getData().clear();
        if (data.isEmpty()) {
            chart.setVisible(false);
            label.setVisible(true);
            return;
        }
        label.setVisible(false);
        chart.setVisible(true);

        data.forEach((k, v) ->
                chart.getData().add(new PieChart.Data(k, v))
        );
    }

    private void updateBar(BarChart<String, Number> chart, Map<String, Integer> data, Label label) {
        chart.getData().clear();
        if (data.isEmpty()) {
            chart.setVisible(false);
            label.setVisible(true);
            return;
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        data.forEach((k, v) -> s.getData().add(new XYChart.Data<>(k, v)));

        chart.getData().add(s);
        chart.setVisible(true);
        label.setVisible(false);
    }

    private void updateLineInt(LineChart<String, Number> chart, Map<String, Integer> data, Label label) {
        chart.getData().clear();
        if (data.isEmpty()) {
            chart.setVisible(false);
            label.setVisible(true);
            return;
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();

        DateTimeFormatter in = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter out = DateTimeFormatter.ofPattern("MM/yyyy");

        data.forEach((k, v) -> {
            String formatted = k;
            try {
                formatted = YearMonth.parse(k, in).format(out);
            } catch (Exception ignored) {}
            s.getData().add(new XYChart.Data<>(formatted, v));
        });

        chart.getData().add(s);
        chart.setVisible(true);
        label.setVisible(false);
    }

    private void updateLineDouble(LineChart<String, Number> chart, Map<String, Double> data, Label label) {
        chart.getData().clear();
        if (data.isEmpty()) {
            chart.setVisible(false);
            label.setVisible(true);
            return;
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();

        DateTimeFormatter in = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter out = DateTimeFormatter.ofPattern("MM/yyyy");

        data.forEach((k, v) -> {
            String formatted = k;
            try {
                formatted = YearMonth.parse(k, in).format(out);
            } catch (Exception ignored) {}
            s.getData().add(new XYChart.Data<>(formatted, v));
        });

        chart.getData().add(s);
        chart.setVisible(true);
        label.setVisible(false);
    }
}