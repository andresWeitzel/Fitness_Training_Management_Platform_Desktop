package com.fitnesstraining.controller;

import com.fitnesstraining.analytics.dto.DebtRow;
import com.fitnesstraining.analytics.dto.MembershipExpiringRow;
import com.fitnesstraining.analytics.dto.OccupancyDayRow;
import com.fitnesstraining.analytics.dto.RevenueRow;
import com.fitnesstraining.analytics.service.AnalyticsService;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.shared.exception.AppException;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsChartsController {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.of("es", "AR"));

    private final AnalyticsService analyticsService;

    @FXML private Label feedbackLabel;
    @FXML private javafx.scene.layout.HBox feedbackBanner;
    @FXML private Button feedbackDismissButton;

    @FXML private StackPane occupancyChartHost;
    @FXML private StackPane revenueChartHost;
    @FXML private StackPane expiringChartHost;
    @FXML private StackPane debtChartHost;

    public AnalyticsChartsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @FXML
    private void initialize() {
        onRefreshCharts();
    }

    @FXML
    public void onDismissFeedback() {
        hideFeedback();
    }

    @FXML
    public void onRefreshCharts() {
        runSafe(() -> {
            LocalDate today = LocalDate.now();
            renderOccupancyChart(analyticsService.listOccupancyByDay(today.minusDays(13), today));
            renderRevenueChart(analyticsService.listRevenue(today.minusDays(29), today));
            renderExpiringChart(analyticsService.listExpiringMemberships(14));
            renderDebtChart(analyticsService.listBlockingDebts());
            showFeedback("Gráficos actualizados.");
        });
    }

    private void renderOccupancyChart(List<OccupancyDayRow> rows) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Cantidad");
        yAxis.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setCategoryGap(8);
        chart.setBarGap(2);
        chart.getStyleClass().add("analytics-chart");

        XYChart.Series<String, Number> entries = new XYChart.Series<>();
        entries.setName("Ingresos");
        XYChart.Series<String, Number> unique = new XYChart.Series<>();
        unique.setName("Únicos");
        DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("dd/MM");
        for (OccupancyDayRow row : rows) {
            String label = shortDate.format(row.day());
            entries.getData().add(new XYChart.Data<>(label, row.entries()));
            unique.getData().add(new XYChart.Data<>(label, row.uniqueClients()));
        }
        chart.getData().addAll(entries, unique);
        setChartOrEmpty(occupancyChartHost, chart, rows.stream().mapToLong(OccupancyDayRow::entries).sum() == 0,
                "Sin check-ins en los últimos 14 días.");
    }

    private void renderRevenueChart(List<RevenueRow> rows) {
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        byType.put("Membresía", BigDecimal.ZERO);
        byType.put("Pase diario", BigDecimal.ZERO);
        byType.put("Mora / recargo", BigDecimal.ZERO);
        for (RevenueRow row : rows) {
            String key = labelForType(row.type());
            byType.merge(key, row.amount() == null ? BigDecimal.ZERO : row.amount(), BigDecimal::add);
        }
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setAnimated(false);
        chart.getStyleClass().add("analytics-chart");
        boolean any = false;
        for (Map.Entry<String, BigDecimal> entry : byType.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            any = true;
            chart.getData().add(new PieChart.Data(
                    entry.getKey() + " (" + formatMoney(entry.getValue()) + ")",
                    entry.getValue().doubleValue()));
        }
        setChartOrEmpty(revenueChartHost, chart, !any, "Sin cobros en los últimos 30 días.");
    }

    private void renderExpiringChart(List<MembershipExpiringRow> rows) {
        Map<String, Integer> byUrgency = new LinkedHashMap<>();
        byUrgency.put("Crítico", 0);
        byUrgency.put("Pronto", 0);
        byUrgency.put("A tiempo", 0);
        for (MembershipExpiringRow row : rows) {
            String key = row.urgency() == null ? "A tiempo" : row.urgency();
            byUrgency.merge(key, 1, Integer::sum);
        }
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Membresías");
        yAxis.setMinorTickVisible(false);
        yAxis.setTickUnit(1);
        yAxis.setMinorTickCount(0);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.getStyleClass().add("analytics-chart");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Cantidad");
        byUrgency.forEach((label, count) -> series.getData().add(new XYChart.Data<>(label, count)));
        chart.getData().add(series);
        setChartOrEmpty(expiringChartHost, chart, rows.isEmpty(), "Sin vencimientos en los próximos 14 días.");
    }

    private void renderDebtChart(List<DebtRow> rows) {
        Map<String, BigDecimal> bySeverity = new LinkedHashMap<>();
        bySeverity.put("Alta", BigDecimal.ZERO);
        bySeverity.put("Media", BigDecimal.ZERO);
        bySeverity.put("Baja", BigDecimal.ZERO);
        for (DebtRow row : rows) {
            String key = row.severity() == null ? "Baja" : row.severity();
            bySeverity.merge(key, row.amount() == null ? BigDecimal.ZERO : row.amount(), BigDecimal::add);
        }
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setAnimated(false);
        chart.getStyleClass().add("analytics-chart");
        boolean any = false;
        for (Map.Entry<String, BigDecimal> entry : bySeverity.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            any = true;
            chart.getData().add(new PieChart.Data(
                    entry.getKey() + " (" + formatMoney(entry.getValue()) + ")",
                    entry.getValue().doubleValue()));
        }
        setChartOrEmpty(debtChartHost, chart, !any, "Sin mora abierta.");
    }

    private static void setChartOrEmpty(StackPane host, Region chart, boolean empty, String emptyMessage) {
        host.getChildren().clear();
        if (empty) {
            Label emptyLabel = new Label(emptyMessage);
            emptyLabel.getStyleClass().add("muted");
            emptyLabel.setWrapText(true);
            StackPane.setAlignment(emptyLabel, Pos.CENTER);
            host.getChildren().add(emptyLabel);
            return;
        }
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        host.getChildren().add(chart);
    }

    private void runSafe(Runnable action) {
        try {
            action.run();
        } catch (AppException ex) {
            showFeedback(ex.getMessage());
        } catch (Exception ex) {
            showFeedback("No se pudo actualizar los gráficos.");
        }
    }

    private void showFeedback(String message) {
        feedbackLabel.setText(message);
        feedbackBanner.setVisible(true);
        feedbackBanner.setManaged(true);
    }

    private void hideFeedback() {
        feedbackBanner.setVisible(false);
        feedbackBanner.setManaged(false);
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        return MONEY.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    private static String labelForType(PaymentType type) {
        if (type == null) {
            return "—";
        }
        return switch (type) {
            case MEMBERSHIP -> "Membresía";
            case LATE_FEE -> "Mora / recargo";
            case DAILY_PASS -> "Pase diario";
        };
    }
}
