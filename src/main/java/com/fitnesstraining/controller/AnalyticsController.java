package com.fitnesstraining.controller;

import com.fitnesstraining.analytics.dto.DebtRow;
import com.fitnesstraining.analytics.dto.MembershipExpiringRow;
import com.fitnesstraining.analytics.dto.OccupancyDayRow;
import com.fitnesstraining.analytics.dto.RevenueRow;
import com.fitnesstraining.analytics.service.AnalyticsService;
import com.fitnesstraining.app.AppContext;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentType;
import com.fitnesstraining.shared.exception.AppException;
import com.fitnesstraining.shared.export.CsvExporter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AnalyticsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.of("es", "AR"));

    private final AnalyticsService analyticsService;
    private final AppContext appContext;

    @FXML private Label feedbackLabel;
    @FXML private javafx.scene.layout.HBox feedbackBanner;
    @FXML private Button feedbackDismissButton;
    @FXML private TabPane analyticsTabs;

    @FXML private Spinner<Integer> expiringDaysSpinner;
    @FXML private Button expiring7Button;
    @FXML private Button expiring14Button;
    @FXML private Button expiring30Button;
    @FXML private Label expiringSummaryLabel;
    @FXML private Label expiringCountKpi;
    @FXML private Label expiringCriticalKpi;
    @FXML private Label expiringValueKpi;
    @FXML private TableView<MembershipExpiringRow> expiringTable;
    @FXML private TableColumn<MembershipExpiringRow, String> expiringUrgencyColumn;
    @FXML private TableColumn<MembershipExpiringRow, String> expiringClientColumn;
    @FXML private TableColumn<MembershipExpiringRow, String> expiringDocumentColumn;
    @FXML private TableColumn<MembershipExpiringRow, String> expiringPlanColumn;
    @FXML private TableColumn<MembershipExpiringRow, String> expiringPriceColumn;
    @FXML private TableColumn<MembershipExpiringRow, String> expiringEndsColumn;
    @FXML private TableColumn<MembershipExpiringRow, String> expiringDaysColumn;

    @FXML private Label debtSummaryLabel;
    @FXML private Label debtCountKpi;
    @FXML private Label debtTotalKpi;
    @FXML private Label debtHighKpi;
    @FXML private TableView<DebtRow> debtTable;
    @FXML private TableColumn<DebtRow, String> debtSeverityColumn;
    @FXML private TableColumn<DebtRow, String> debtClientColumn;
    @FXML private TableColumn<DebtRow, String> debtDocumentColumn;
    @FXML private TableColumn<DebtRow, String> debtTypeColumn;
    @FXML private TableColumn<DebtRow, String> debtAmountColumn;
    @FXML private TableColumn<DebtRow, String> debtDueColumn;
    @FXML private TableColumn<DebtRow, String> debtDaysColumn;

    @FXML private DatePicker revenueFromPicker;
    @FXML private DatePicker revenueToPicker;
    @FXML private Label revenueSummaryLabel;
    @FXML private Label revenueCountKpi;
    @FXML private Label revenueTotalKpi;
    @FXML private Label revenueAvgKpi;
    @FXML private TableView<RevenueRow> revenueTable;
    @FXML private TableColumn<RevenueRow, String> revenueClientColumn;
    @FXML private TableColumn<RevenueRow, String> revenueDocumentColumn;
    @FXML private TableColumn<RevenueRow, String> revenueTypeColumn;
    @FXML private TableColumn<RevenueRow, String> revenueAmountColumn;
    @FXML private TableColumn<RevenueRow, String> revenueMethodColumn;
    @FXML private TableColumn<RevenueRow, String> revenuePaidColumn;

    @FXML private DatePicker occupancyFromPicker;
    @FXML private DatePicker occupancyToPicker;
    @FXML private Label occupancySummaryLabel;
    @FXML private Label occupancyEntriesKpi;
    @FXML private Label occupancyPeakKpi;
    @FXML private Label occupancyAvgKpi;
    @FXML private TableView<OccupancyDayRow> occupancyTable;
    @FXML private TableColumn<OccupancyDayRow, String> occupancyDayColumn;
    @FXML private TableColumn<OccupancyDayRow, String> occupancyEntriesColumn;
    @FXML private TableColumn<OccupancyDayRow, String> occupancyUniqueColumn;

    public AnalyticsController(AnalyticsService analyticsService, AppContext appContext) {
        this.analyticsService = analyticsService;
        this.appContext = appContext;
    }

    @FXML
    private void initialize() {
        SpinnerValueFactory.IntegerSpinnerValueFactory daysFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 7);
        expiringDaysSpinner.setValueFactory(daysFactory);
        expiringDaysSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                syncExpiringChips(newV);
            }
        });

        LocalDate today = LocalDate.now();
        revenueFromPicker.setValue(today.minusDays(29));
        revenueToPicker.setValue(today);
        occupancyFromPicker.setValue(today.minusDays(13));
        occupancyToPicker.setValue(today);

        configureExpiringColumns();
        configureDebtColumns();
        configureRevenueColumns();
        configureOccupancyColumns();

        expiringTable.setPlaceholder(new Label("No hay membresías por vencer en esta ventana."));
        debtTable.setPlaceholder(new Label("No hay deuda que bloquee el acceso."));
        revenueTable.setPlaceholder(new Label("No hay cobros en el rango elegido."));
        occupancyTable.setPlaceholder(new Label("No hay ingresos registrados en el rango."));

        onGenerateExpiring();
        onGenerateDebt();
        onGenerateRevenue();
        onGenerateOccupancy();
    }

    @FXML
    public void onDismissFeedback() {
        hideFeedback();
    }

    @FXML
    public void onExpiringWindow7() {
        setExpiringWindow(7);
    }

    @FXML
    public void onExpiringWindow14() {
        setExpiringWindow(14);
    }

    @FXML
    public void onExpiringWindow30() {
        setExpiringWindow(30);
    }

    @FXML
    public void onRevenueLast7() {
        LocalDate today = LocalDate.now();
        revenueFromPicker.setValue(today.minusDays(6));
        revenueToPicker.setValue(today);
        onGenerateRevenue();
    }

    @FXML
    public void onRevenueLast30() {
        LocalDate today = LocalDate.now();
        revenueFromPicker.setValue(today.minusDays(29));
        revenueToPicker.setValue(today);
        onGenerateRevenue();
    }

    @FXML
    public void onOccupancyLast7() {
        LocalDate today = LocalDate.now();
        occupancyFromPicker.setValue(today.minusDays(6));
        occupancyToPicker.setValue(today);
        onGenerateOccupancy();
    }

    @FXML
    public void onOccupancyLast14() {
        LocalDate today = LocalDate.now();
        occupancyFromPicker.setValue(today.minusDays(13));
        occupancyToPicker.setValue(today);
        onGenerateOccupancy();
    }

    @FXML
    public void onGenerateExpiring() {
        runSafe(() -> {
            int days = expiringDaysSpinner.getValue() == null ? 7 : expiringDaysSpinner.getValue();
            syncExpiringChips(days);
            List<MembershipExpiringRow> rows = analyticsService.listExpiringMemberships(days);
            expiringTable.setItems(FXCollections.observableArrayList(rows));

            long critical = rows.stream().filter(r -> r.daysUntilExpiry() <= 3).count();
            BigDecimal value = rows.stream()
                    .map(r -> r.planPrice() == null ? BigDecimal.ZERO : r.planPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            expiringCountKpi.setText(String.valueOf(rows.size()));
            expiringCriticalKpi.setText(String.valueOf(critical));
            expiringValueKpi.setText(formatMoney(value));
            expiringSummaryLabel.setText(rows.isEmpty()
                    ? "Sin membresías activas que venzan en los próximos " + days + " días."
                    : rows.size() + " membresía(s) · " + critical + " crítica(s) · valor estimado de planes "
                    + formatMoney(value));
            showFeedback("Reporte de vencimientos actualizado.");
        });
    }

    @FXML
    public void onGenerateDebt() {
        runSafe(() -> {
            List<DebtRow> rows = analyticsService.listBlockingDebts();
            debtTable.setItems(FXCollections.observableArrayList(rows));
            BigDecimal total = rows.stream()
                    .map(DebtRow::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long high = rows.stream().filter(r -> "Alta".equals(r.severity())).count();

            debtCountKpi.setText(String.valueOf(rows.size()));
            debtTotalKpi.setText(formatMoney(total));
            debtHighKpi.setText(String.valueOf(high));
            debtSummaryLabel.setText(rows.isEmpty()
                    ? "No hay mora ni pagos vencidos pendientes."
                    : rows.size() + " deuda(s) · total " + formatMoney(total)
                    + " · " + high + " con severidad alta");
            showFeedback("Reporte de mora actualizado.");
        });
    }

    @FXML
    public void onGenerateRevenue() {
        runSafe(() -> {
            List<RevenueRow> rows = analyticsService.listRevenue(
                    revenueFromPicker.getValue(), revenueToPicker.getValue());
            revenueTable.setItems(FXCollections.observableArrayList(rows));
            BigDecimal total = rows.stream()
                    .map(RevenueRow::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = rows.isEmpty()
                    ? BigDecimal.ZERO
                    : total.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);

            revenueCountKpi.setText(String.valueOf(rows.size()));
            revenueTotalKpi.setText(formatMoney(total));
            revenueAvgKpi.setText(formatMoney(avg));
            revenueSummaryLabel.setText(rows.isEmpty()
                    ? "Sin cobros PAID en el rango seleccionado."
                    : rows.size() + " cobro(s) · total " + formatMoney(total)
                    + " · promedio " + formatMoney(avg));
            showFeedback("Reporte de ingresos actualizado.");
        });
    }

    @FXML
    public void onGenerateOccupancy() {
        runSafe(() -> {
            List<OccupancyDayRow> rows = analyticsService.listOccupancyByDay(
                    occupancyFromPicker.getValue(), occupancyToPicker.getValue());
            occupancyTable.setItems(FXCollections.observableArrayList(rows));
            long entries = rows.stream().mapToLong(OccupancyDayRow::entries).sum();
            long peak = rows.stream().mapToLong(OccupancyDayRow::entries).max().orElse(0);
            double avg = rows.isEmpty() ? 0 : (double) entries / rows.size();

            occupancyEntriesKpi.setText(String.valueOf(entries));
            occupancyPeakKpi.setText(String.valueOf(peak));
            occupancyAvgKpi.setText(String.format(Locale.ROOT, "%.1f", avg));
            occupancySummaryLabel.setText(rows.isEmpty()
                    ? "Sin check-ins en el rango."
                    : entries + " ingreso(s) · pico " + peak + " · promedio diario "
                    + String.format(Locale.ROOT, "%.1f", avg));
            showFeedback("Reporte de ocupación actualizado.");
        });
    }

    @FXML
    public void onExportExpiring() {
        exportTable(
                "Membresias_por_vencer",
                List.of("Urgencia", "Cliente", "Documento", "Plan", "Precio", "Vence", "Días"),
                expiringTable.getItems().stream()
                        .map(row -> List.of(
                                nullToEmpty(row.urgency()),
                                nullToEmpty(row.clientName()),
                                nullToEmpty(row.clientDocument()),
                                nullToEmpty(row.planName()),
                                formatMoneyPlain(row.planPrice()),
                                formatExcelDate(row.endsOn()),
                                String.valueOf(row.daysUntilExpiry())))
                        .toList());
    }

    @FXML
    public void onExportDebt() {
        exportTable(
                "Mora_y_deuda",
                List.of("Severidad", "Cliente", "Documento", "Tipo", "Monto", "Vencimiento", "Días"),
                debtTable.getItems().stream()
                        .map(row -> List.of(
                                nullToEmpty(row.severity()),
                                nullToEmpty(row.clientName()),
                                nullToEmpty(row.clientDocument()),
                                labelForType(row.type()),
                                formatMoneyPlain(row.amount()),
                                formatExcelDate(row.dueOn()),
                                String.valueOf(row.daysOverdue())))
                        .toList());
    }

    @FXML
    public void onExportRevenue() {
        exportTable(
                "Ingresos",
                List.of("Cliente", "Documento", "Tipo", "Monto", "Medio", "Cobrado"),
                revenueTable.getItems().stream()
                        .map(row -> List.of(
                                nullToEmpty(row.clientName()),
                                nullToEmpty(row.clientDocument()),
                                labelForType(row.type()),
                                formatMoneyPlain(row.amount()),
                                labelForMethod(row.method()),
                                formatExcelDate(row.paidOn())))
                        .toList());
    }

    @FXML
    public void onExportOccupancy() {
        exportTable(
                "Ocupacion",
                List.of("Día", "Ingresos", "Clientes únicos"),
                occupancyTable.getItems().stream()
                        .map(row -> List.of(
                                formatExcelDate(row.day()),
                                String.valueOf(row.entries()),
                                String.valueOf(row.uniqueClients())))
                        .toList());
    }

    @FXML
    public void onOpenPaymentsFromDebt() {
        DebtRow selected = debtTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Seleccioná una fila de mora para abrir Pagos.");
            return;
        }
        appContext.openPaymentsForClient(selected.clientId());
    }

    private void setExpiringWindow(int days) {
        expiringDaysSpinner.getValueFactory().setValue(days);
        syncExpiringChips(days);
        onGenerateExpiring();
    }

    private void syncExpiringChips(int days) {
        setChipOn(expiring7Button, days == 7);
        setChipOn(expiring14Button, days == 14);
        setChipOn(expiring30Button, days == 30);
    }

    private static void setChipOn(Button button, boolean on) {
        button.getStyleClass().remove("chip-on");
        if (on) {
            button.getStyleClass().add("chip-on");
        }
    }

    private void configureExpiringColumns() {
        expiringUrgencyColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().urgency()));
        expiringClientColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientName()));
        expiringDocumentColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientDocument()));
        expiringPlanColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().planName()));
        expiringPriceColumn.setCellValueFactory(d -> new SimpleStringProperty(formatMoney(d.getValue().planPrice())));
        expiringEndsColumn.setCellValueFactory(d -> new SimpleStringProperty(formatDate(d.getValue().endsOn())));
        expiringDaysColumn.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().daysUntilExpiry())));
    }

    private void configureDebtColumns() {
        debtSeverityColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().severity()));
        debtClientColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientName()));
        debtDocumentColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientDocument()));
        debtTypeColumn.setCellValueFactory(d -> new SimpleStringProperty(labelForType(d.getValue().type())));
        debtAmountColumn.setCellValueFactory(d -> new SimpleStringProperty(formatMoney(d.getValue().amount())));
        debtDueColumn.setCellValueFactory(d -> new SimpleStringProperty(formatDate(d.getValue().dueOn())));
        debtDaysColumn.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().daysOverdue())));
        debtTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenPaymentsFromDebt();
            }
        });
    }

    private void configureRevenueColumns() {
        revenueClientColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientName()));
        revenueDocumentColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientDocument()));
        revenueTypeColumn.setCellValueFactory(d -> new SimpleStringProperty(labelForType(d.getValue().type())));
        revenueAmountColumn.setCellValueFactory(d -> new SimpleStringProperty(formatMoney(d.getValue().amount())));
        revenueMethodColumn.setCellValueFactory(d -> new SimpleStringProperty(labelForMethod(d.getValue().method())));
        revenuePaidColumn.setCellValueFactory(d -> new SimpleStringProperty(formatDate(d.getValue().paidOn())));
    }

    private void configureOccupancyColumns() {
        occupancyDayColumn.setCellValueFactory(d -> new SimpleStringProperty(formatDate(d.getValue().day())));
        occupancyEntriesColumn.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().entries())));
        occupancyUniqueColumn.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().uniqueClients())));
    }

    private void exportTable(String title, List<String> headers, List<List<String>> rows) {
        if (rows.isEmpty()) {
            showAlert("No hay datos para exportar. Actualizá el reporte primero.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar reporte");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName(CsvExporter.suggestedFileName(title));
        Window owner = analyticsTabs.getScene() == null ? null : analyticsTabs.getScene().getWindow();
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        runSafe(() -> {
            try {
                Path path = file.toPath();
                CsvExporter.write(path, headers, rows);
                showFeedback("Reporte exportado: " + path.getFileName());
            } catch (Exception ex) {
                throw new AppException("No se pudo exportar el archivo.");
            }
        });
    }

    private void runSafe(Runnable action) {
        try {
            action.run();
        } catch (AppException ex) {
            showFeedback(ex.getMessage());
        } catch (Exception ex) {
            showFeedback("No se pudo completar la operación.");
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

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportar");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String formatExcelDate(LocalDate date) {
        return CsvExporter.formatExcelDate(date);
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : DATE_FORMAT.format(date);
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        return MONEY.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    private static String formatMoneyPlain(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    private static String labelForMethod(PaymentMethod method) {
        if (method == null) {
            return "—";
        }
        return switch (method) {
            case CASH -> "Efectivo";
            case TRANSFER -> "Transferencia";
            case CARD -> "Tarjeta";
            case OTHER -> "Otro";
        };
    }
}
