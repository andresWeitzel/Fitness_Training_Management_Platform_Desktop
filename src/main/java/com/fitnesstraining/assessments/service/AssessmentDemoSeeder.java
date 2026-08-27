package com.fitnesstraining.assessments.service;

import com.fitnesstraining.assessments.dto.AssessmentRequest;
import com.fitnesstraining.assessments.model.AssessmentListScope;
import com.fitnesstraining.assessments.repository.AssessmentRepository;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

public class AssessmentDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(AssessmentDemoSeeder.class);

    private final ClientRepository clientRepository;
    private final AssessmentService assessmentService;
    private final UserRepository userRepository;
    private final Clock clock;

    public AssessmentDemoSeeder(
            ClientRepository clientRepository,
            AssessmentService assessmentService,
            UserRepository userRepository,
            Clock clock) {
        this.clientRepository = clientRepository;
        this.assessmentService = assessmentService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public void seedIfEmpty() {
        if (!assessmentService.list("", AssessmentListScope.ALL).isEmpty()) {
            return;
        }
        var clients = clientRepository.findAllActiveRecords();
        if (clients.isEmpty()) {
            return;
        }
        Long trainerId = userRepository.findActiveByUsername("carlos_trainer")
                .map(u -> u.getId())
                .orElse(null);
        if (trainerId == null) {
            return;
        }

        int clientLimit = Math.min(5, clients.size());
        int total = 0;
        for (int c = 0; c < clientLimit; c++) {
            Client client = clients.get(c);
            for (int i = 0; i < 3; i++) {
                long daysAgo = 84L - (c * 7L) - (i * 28L);
                assessmentService.register(
                        new AssessmentRequest(
                                client.getId(),
                                LocalDate.now(clock).minusDays(daysAgo),
                                BigDecimal.valueOf(71.0 + c + (i * 0.8)),
                                BigDecimal.valueOf(167 + c),
                                BigDecimal.valueOf(17.5 + c + i),
                                BigDecimal.valueOf(80 + c + i),
                                BigDecimal.valueOf(96 + c),
                                BigDecimal.valueOf(94 + c),
                                switch (i) {
                                    case 0 -> "Evaluación inicial de demo.";
                                    case 1 -> "Control intermedio — evolución positiva.";
                                    default -> "Control reciente — seguimiento mensual.";
                                }),
                        trainerId);
                total++;
            }
        }
        log.info("Evaluaciones demo sembradas: {}", total);
    }
}
