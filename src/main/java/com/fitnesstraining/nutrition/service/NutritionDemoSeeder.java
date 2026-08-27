package com.fitnesstraining.nutrition.service;

import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.nutrition.dto.HealthRecordRequest;
import com.fitnesstraining.nutrition.dto.NutritionAppointmentRequest;
import com.fitnesstraining.nutrition.dto.NutritionPlanRequest;
import com.fitnesstraining.nutrition.model.NutritionAppointmentListScope;
import com.fitnesstraining.nutrition.model.NutritionPlanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

public class NutritionDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(NutritionDemoSeeder.class);

    private final ClientRepository clientRepository;
    private final NutritionService nutritionService;
    private final UserRepository userRepository;
    private final Clock clock;

    public NutritionDemoSeeder(
            ClientRepository clientRepository,
            NutritionService nutritionService,
            UserRepository userRepository,
            Clock clock) {
        this.clientRepository = clientRepository;
        this.nutritionService = nutritionService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public void seedIfEmpty() {
        if (!nutritionService.listAppointments("", NutritionAppointmentListScope.ALL).isEmpty()) {
            return;
        }
        var clients = clientRepository.findAllActiveRecords();
        if (clients.isEmpty()) {
            return;
        }
        Long nutritionistId = userRepository.findActiveByUsername("maria_nutri")
                .map(u -> u.getId())
                .orElse(null);
        if (nutritionistId == null) {
            return;
        }

        int clientLimit = Math.min(5, clients.size());
        for (int c = 0; c < clientLimit; c++) {
            var client = clients.get(c);

            var past = nutritionService.scheduleAppointment(
                    new NutritionAppointmentRequest(
                            client.getId(),
                            LocalDate.now(clock).minusDays(14 + c),
                            LocalTime.of(9 + c, 30),
                            "Consulta inicial completada."),
                    nutritionistId);
            nutritionService.completeAppointment(past.id());

            nutritionService.scheduleAppointment(
                    new NutritionAppointmentRequest(
                            client.getId(),
                            LocalDate.now(clock).minusDays(7),
                            LocalTime.of(11, 0),
                            "Seguimiento cancelado."),
                    nutritionistId);
            var cancelled = nutritionService.listAppointments(client.getDocumentNumber(), NutritionAppointmentListScope.ALL)
                    .stream()
                    .filter(a -> a.scheduledAt().toLocalDate().equals(LocalDate.now(clock).minusDays(7)))
                    .findFirst()
                    .orElse(null);
            if (cancelled != null) {
                nutritionService.cancelAppointment(cancelled.id());
            }

            nutritionService.scheduleAppointment(
                    new NutritionAppointmentRequest(
                            client.getId(),
                            LocalDate.now(clock).plusDays(2L + c),
                            LocalTime.of(10 + c, 0),
                            "Próximo control programado."),
                    nutritionistId);

            nutritionService.createPlan(
                    new NutritionPlanRequest(
                            client.getId(),
                            "Plan activo · " + client.getFirstName(),
                            "Mejorar hábitos, energía y composición corporal.",
                            "Desayuno: avena + fruta + proteína\nAlmuerzo: plato balanceado\nMerienda: yogur o frutos secos\nCena: liviana con verduras",
                            NutritionPlanStatus.ACTIVE,
                            LocalDate.now(clock).minusWeeks(2),
                            LocalDate.now(clock).plusMonths(1),
                            "Plan vigente de demo."),
                    nutritionistId);

            var archived = nutritionService.createPlan(
                    new NutritionPlanRequest(
                            client.getId(),
                            "Plan anterior · " + client.getFirstName(),
                            "Etapa de adaptación inicial.",
                            "Pautas generales de hidratación y porciones.",
                            NutritionPlanStatus.ACTIVE,
                            LocalDate.now(clock).minusMonths(3),
                            LocalDate.now(clock).minusMonths(1),
                            "Plan archivado de demo."),
                    nutritionistId);
            nutritionService.archivePlan(archived.id());

            nutritionService.addHealthRecord(
                    new HealthRecordRequest(
                            client.getId(),
                            LocalDate.now(clock).minusDays(60),
                            c == 0 ? "Nueces, maní" : null,
                            c == 1 ? "Vegetariano" : "Sin azúcar añadida",
                            "Sin antecedentes relevantes.",
                            null,
                            "Primera ficha de salud."),
                    nutritionistId);
            nutritionService.addHealthRecord(
                    new HealthRecordRequest(
                            client.getId(),
                            LocalDate.now(clock).minusDays(30),
                            null,
                            "Reduce lactosa",
                            null,
                            c == 2 ? "Suplemento vitamina D" : null,
                            "Actualización de restricciones."),
                    nutritionistId);
            nutritionService.addHealthRecord(
                    new HealthRecordRequest(
                            client.getId(),
                            LocalDate.now(clock).minusDays(7),
                            null,
                            null,
                            "Control anual al día.",
                            null,
                            "Control reciente de demo."),
                    nutritionistId);
        }
        log.info("Datos demo de nutrición sembrados para {} clientes.", clientLimit);
    }
}
