package com.fitnesstraining.controller;

import com.fitnesstraining.app.DetailWindows;
import com.fitnesstraining.payments.dto.PaymentView;
import com.fitnesstraining.payments.model.PaymentMethod;
import com.fitnesstraining.payments.model.PaymentStatus;
import com.fitnesstraining.payments.model.PaymentType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PaymentDetailController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    private static final double CARD_WIDTH = 460;

    @FXML private StackPane rootPane;
    @FXML private Label statusBadge;
    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label typeLabel;
    @FXML private Label amountLabel;
    @FXML private Label dateLabel;
    @FXML private Label methodLabel;
    @FXML private Label membershipLabel;
    @FXML private Label documentLabel;
    @FXML private Label notesLabel;
    @FXML private Label helpLabel;

    private Stage stage;

    public static void open(Window owner, PaymentView view) {
        if (view == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    PaymentDetailController.class.getResource("/views/payment-detail.fxml"));
            Parent root = loader.load();
            PaymentDetailController controller = loader.getController();
            controller.bind(view);
            controller.stage = DetailWindows.open(owner, root, "Resumen del cobro", CARD_WIDTH);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el resumen del cobro.", ex);
        }
    }

    private void bind(PaymentView view) {
        titleLabel.setText(view.clientName());
        summaryLabel.setText("Cobro #" + view.id() + " · " + view.clientDocument());
        statusBadge.setText(labelForStatus(view.status(), view.overdue()));
        statusBadge.getStyleClass().setAll(badgeClassForStatus(view.status(), view.overdue()));

        typeLabel.setText(labelForType(view.type()));
        amountLabel.setText(formatMoney(view.amount()));
        dateLabel.setText(formatDateLine(view));
        methodLabel.setText(labelForMethod(view.method()));
        membershipLabel.setText(view.membershipPlanName() == null || view.membershipPlanName().isBlank()
                ? "Sin membresía vinculada"
                : view.membershipPlanName());
        documentLabel.setText(view.clientDocument());
        notesLabel.setText(view.notes() == null || view.notes().isBlank()
                ? "Sin notas."
                : view.notes());
        helpLabel.setText(statusHelp(view));
    }

    @FXML
    public void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private static String formatDateLine(PaymentView view) {
        if (view.status() == PaymentStatus.PAID) {
            return view.paidAt() == null ? "Cobrado" : "Cobrado " + formatDateTime(view.paidAt());
        }
        if (view.overdue()) {
            return view.dueAt() == null ? "En mora" : "Venció " + formatDate(view.dueAt());
        }
        if (view.dueAt() != null) {
            return "Vence " + formatDate(view.dueAt());
        }
        return switch (view.status()) {
            case PENDING -> "Pendiente";
            case CANCELLED -> "Cancelado";
            case PAID -> "Cobrado";
        };
    }

    private static String statusHelp(PaymentView view) {
        if (view.overdue()) {
            return "En mora: cobro pendiente vencido. Marcarlo cobrado limpia la deuda en recepción.";
        }
        return switch (view.status()) {
            case PAID -> "Cobrado. No genera deuda para el check-in.";
            case PENDING -> "Pendiente de cobro. Puede marcarlo cobrado o cancelarlo desde el formulario.";
            case CANCELLED -> "Cancelado. No genera deuda para el check-in.";
        };
    }

    private static String formatMoney(BigDecimal amount) {
        return amount == null ? "—" : CURRENCY.format(amount);
    }

    private static String formatDate(OffsetDateTime value) {
        return value == null ? "—" : DATE_FORMAT.format(value.toLocalDate());
    }

    private static String formatDateTime(OffsetDateTime value) {
        return value == null ? "—" : DATE_TIME_FORMAT.format(value);
    }

    private static String labelForType(PaymentType type) {
        if (type == null) {
            return "—";
        }
        return switch (type) {
            case MEMBERSHIP -> "Membresía";
            case DAILY_PASS -> "Ingreso diario";
            case LATE_FEE -> "Mora / recargo";
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

    private static String labelForStatus(PaymentStatus status, boolean overdue) {
        if (overdue) {
            return "En mora";
        }
        return switch (status) {
            case PAID -> "Cobrado";
            case PENDING -> "Pendiente";
            case CANCELLED -> "Cancelado";
        };
    }

    private static String badgeClassForStatus(PaymentStatus status, boolean overdue) {
        if (overdue) {
            return "badge-overdue";
        }
        return switch (status) {
            case PAID -> "badge-paid";
            case PENDING -> "badge-pending";
            case CANCELLED -> "badge-cancelled";
        };
    }
}
