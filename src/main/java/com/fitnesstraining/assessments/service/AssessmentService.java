package com.fitnesstraining.assessments.service;

import com.fitnesstraining.assessments.dto.AssessmentClientOption;
import com.fitnesstraining.assessments.dto.AssessmentRequest;
import com.fitnesstraining.assessments.dto.AssessmentSummary;
import com.fitnesstraining.assessments.dto.AssessmentView;
import com.fitnesstraining.assessments.model.AssessmentListScope;
import com.fitnesstraining.assessments.model.PhysicalAssessment;
import com.fitnesstraining.assessments.repository.AssessmentRepository;
import com.fitnesstraining.auth.model.User;
import com.fitnesstraining.auth.repository.UserRepository;
import com.fitnesstraining.members.model.Client;
import com.fitnesstraining.members.model.ClientStatus;
import com.fitnesstraining.members.model.ClientListScope;
import com.fitnesstraining.members.repository.AccessCredentialRepository;
import com.fitnesstraining.members.repository.ClientRepository;
import com.fitnesstraining.shared.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final ClientRepository clientRepository;
    private final AccessCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            ClientRepository clientRepository,
            AccessCredentialRepository credentialRepository,
            UserRepository userRepository,
            Clock clock) {
        this.assessmentRepository = assessmentRepository;
        this.clientRepository = clientRepository;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public List<AssessmentSummary> list(String term, AssessmentListScope scope) {
        return list(term, scope, null);
    }

    public List<AssessmentSummary> list(String term, AssessmentListScope scope, Long clientId) {
        OffsetDateTime since = sinceForScope(scope);
        return assessmentRepository.list(term, scope, since, clientId).stream()
                .map(this::toSummary)
                .toList();
    }

    public AssessmentView get(Long id) {
        PhysicalAssessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Evaluación no encontrada."));
        return toView(assessment);
    }

    public Optional<String> latestSummaryForClient(Long clientId) {
        if (clientId == null) {
            return Optional.empty();
        }
        return assessmentRepository.findLatestByClientId(clientId).map(assessment -> {
            String date = assessment.getAssessedAt().toLocalDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            BigDecimal bmi = computeBmi(assessment.getWeightKg(), assessment.getHeightCm());
            if (assessment.getWeightKg() != null && bmi != null) {
                return date + " · " + assessment.getWeightKg().stripTrailingZeros().toPlainString()
                        + " kg · IMC " + bmi.toPlainString();
            }
            if (assessment.getWeightKg() != null) {
                return date + " · " + assessment.getWeightKg().stripTrailingZeros().toPlainString() + " kg";
            }
            return date;
        });
    }

    public List<AssessmentClientOption> listActiveClients() {
        return clientRepository.list(ClientListScope.ACTIVE).stream()
                .sorted(Comparator.comparing(Client::fullName, String.CASE_INSENSITIVE_ORDER))
                .map(client -> new AssessmentClientOption(
                        client.getId(),
                        client.getDocumentNumber(),
                        client.fullName(),
                        credentialRepository.findClientNumber(client.getId()).orElse(null)))
                .toList();
    }

    public AssessmentView register(AssessmentRequest request, Long assessorUserId) {
        validateRequest(request);
        Client client = clientRepository.findActiveById(request.clientId())
                .filter(c -> c.getStatus() == ClientStatus.ACTIVE)
                .orElseThrow(() -> new ValidationException("Seleccione un cliente activo."));
        User assessor = userRepository.findById(assessorUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new ValidationException("Usuario evaluador no válido."));

        PhysicalAssessment assessment = new PhysicalAssessment();
        assessment.setClient(client);
        assessment.setAssessedBy(assessor);
        assessment.setAssessedAt(toAssessedAt(request.assessedOn()));
        assessment.setWeightKg(request.weightKg());
        assessment.setHeightCm(request.heightCm());
        assessment.setBodyFatPct(request.bodyFatPct());
        assessment.setWaistCm(request.waistCm());
        assessment.setHipCm(request.hipCm());
        assessment.setChestCm(request.chestCm());
        assessment.setNotes(trimToNull(request.notes()));

        return toView(assessmentRepository.save(assessment));
    }

    public static BigDecimal computeBmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null
                || weightKg.compareTo(BigDecimal.ZERO) <= 0
                || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    public static String labelForBmi(BigDecimal bmi) {
        if (bmi == null) {
            return "—";
        }
        double value = bmi.doubleValue();
        if (value < 18.5) {
            return "Bajo peso";
        }
        if (value < 25) {
            return "Normal";
        }
        if (value < 30) {
            return "Sobrepeso";
        }
        return "Obesidad";
    }

    private OffsetDateTime sinceForScope(AssessmentListScope scope) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return switch (scope) {
            case ALL -> null;
            case LAST_30_DAYS -> now.minusDays(30);
            case LAST_90_DAYS -> now.minusDays(90);
        };
    }

    private OffsetDateTime toAssessedAt(LocalDate date) {
        LocalDate assessedOn = date == null ? LocalDate.now(clock) : date;
        ZoneOffset offset = clock.getZone().getRules().getOffset(clock.instant());
        return assessedOn.atTime(LocalTime.NOON).atOffset(offset);
    }

    private void validateRequest(AssessmentRequest request) {
        if (request.clientId() == null) {
            throw new ValidationException("Seleccione un cliente.");
        }
        if (request.weightKg() == null && request.heightCm() == null && request.bodyFatPct() == null) {
            throw new ValidationException("Indique al menos peso, altura o % grasa.");
        }
        if (request.weightKg() != null && request.weightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El peso debe ser mayor a cero.");
        }
        if (request.heightCm() != null && request.heightCm().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("La altura debe ser mayor a cero.");
        }
        if (request.bodyFatPct() != null
                && (request.bodyFatPct().compareTo(BigDecimal.ZERO) < 0
                || request.bodyFatPct().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new ValidationException("El % de grasa debe estar entre 0 y 100.");
        }
        validateNonNegative(request.waistCm(), "cintura");
        validateNonNegative(request.hipCm(), "cadera");
        validateNonNegative(request.chestCm(), "tórax");
    }

    private static void validateNonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("La medida de " + label + " no puede ser negativa.");
        }
    }

    private AssessmentSummary toSummary(PhysicalAssessment assessment) {
        Client client = assessment.getClient();
        return new AssessmentSummary(
                assessment.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                assessment.getAssessedAt(),
                assessment.getWeightKg(),
                assessment.getHeightCm(),
                computeBmi(assessment.getWeightKg(), assessment.getHeightCm()),
                assessment.getBodyFatPct(),
                assessment.getAssessedBy().getDisplayName());
    }

    private AssessmentView toView(PhysicalAssessment assessment) {
        Client client = assessment.getClient();
        return new AssessmentView(
                assessment.getId(),
                client.getId(),
                client.getDocumentNumber(),
                client.fullName(),
                credentialRepository.findClientNumber(client.getId()).orElse(null),
                assessment.getAssessedBy().getId(),
                assessment.getAssessedBy().getDisplayName(),
                assessment.getAssessedAt(),
                assessment.getWeightKg(),
                assessment.getHeightCm(),
                computeBmi(assessment.getWeightKg(), assessment.getHeightCm()),
                assessment.getBodyFatPct(),
                assessment.getWaistCm(),
                assessment.getHipCm(),
                assessment.getChestCm(),
                assessment.getNotes());
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
