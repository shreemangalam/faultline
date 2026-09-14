package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.persistence.RecoveryAttemptRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventDependencyEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventDependencyRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecoveryEventService {

    private final RecoveryEventRepository eventRepository;
    private final RecoveryEventAuditRepository auditRepository;
    private final RecoveryEventDependencyRepository dependencyRepository;
    private final RecoveryAttemptRepository attemptRepository;
    private final Clock clock;
    private final CircuitBreakerService circuitBreakerService;

    public RecoveryEventService(RecoveryEventRepository eventRepository, RecoveryEventAuditRepository auditRepository, RecoveryEventDependencyRepository dependencyRepository, RecoveryAttemptRepository attemptRepository, Clock clock, CircuitBreakerService circuitBreakerService) {
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
        this.dependencyRepository = dependencyRepository;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
        this.circuitBreakerService = circuitBreakerService;
    }

    public Submission submit(CreateRecoveryEventCommand command) {
        String fingerprint = RecoveryEventFingerprint.calculate(command);
        var existing = eventRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key is already associated with a different event.");
            }
            return new Submission(details(existing.get()), false);
        }

        List<RecoveryEventEntity> prerequisites = prerequisites(command);
        Instant now = clock.instant();
        circuitBreakerService.ensureExists(command.targetSystem(), now);
        RecoveryEventEntity failedPrerequisite = prerequisites.stream()
                .filter(event -> isTerminalFailure(event.getState()))
                .findFirst()
                .orElse(null);
        boolean prerequisitesComplete = prerequisites.stream().allMatch(event -> event.getState() == RecoveryEventState.COMPLETED);
        RecoveryEventState initialState = failedPrerequisite != null
                ? RecoveryEventState.FAILED_DEPENDENCY
                : prerequisitesComplete ? RecoveryEventState.ELIGIBLE : RecoveryEventState.BLOCKED;
        RecoveryEventEntity event = RecoveryEventEntity.receive(
                UUID.randomUUID(), command.idempotencyKey(), fingerprint, command.correlationId(), command.targetSystem(),
                command.payloadReference(), command.priority(), command.deadlineAt(), initialState, now);
        eventRepository.save(event);
        dependencyRepository.saveAll(prerequisites.stream()
                .map(prerequisite -> new RecoveryEventDependencyEntity(UUID.randomUUID(), event.getId(), prerequisite.getId()))
                .toList());
        auditRepository.saveAll(List.of(
                new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.RECEIVED, "Event accepted for recovery.", now),
                new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), initialState, initialReason(
                        initialState, prerequisites.size(), failedPrerequisite), now)
        ));
        return new Submission(details(event), true);
    }

    @Transactional(readOnly = true)
    public RecoveryEventDetails get(UUID eventId) {
        RecoveryEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery event was not found."));
        return details(event);
    }

    @Transactional(readOnly = true)
    public List<RecoveryEventDetails> list(RecoveryEventState state) {
        List<RecoveryEventEntity> events = state == null ? eventRepository.findAll() : eventRepository.findByStateOrderByCreatedAtAsc(state);
        return events.stream().map(this::details).toList();
    }

    private List<RecoveryEventEntity> prerequisites(CreateRecoveryEventCommand command) {
        if (new HashSet<>(command.prerequisiteEventIds()).size() != command.prerequisiteEventIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prerequisite event IDs must be unique.");
        }
        return command.prerequisiteEventIds().stream().map(prerequisiteEventId -> {
            RecoveryEventEntity prerequisite = eventRepository.findById(prerequisiteEventId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prerequisite recovery event was not found."));
            if (!prerequisite.getCorrelationId().equals(command.correlationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prerequisites must use the same correlation ID.");
            }
            return prerequisite;
        }).toList();
    }

    private String initialReason(
            RecoveryEventState initialState,
            int prerequisiteCount,
            RecoveryEventEntity failedPrerequisite) {
        if (initialState == RecoveryEventState.ELIGIBLE) return "No incomplete prerequisites are registered.";
        if (initialState == RecoveryEventState.FAILED_DEPENDENCY) {
            return "Prerequisite " + failedPrerequisite.getId() + " is " + failedPrerequisite.getState()
                    + "; recovery cannot continue.";
        }
        return "Waiting for " + prerequisiteCount + " prerequisite event(s).";
    }

    private boolean isTerminalFailure(RecoveryEventState state) {
        return state == RecoveryEventState.EXHAUSTED
                || state == RecoveryEventState.FAILED_FINAL
                || state == RecoveryEventState.FAILED_DEPENDENCY;
    }

    private RecoveryEventDetails details(RecoveryEventEntity event) {
        List<UUID> prerequisiteEventIds = dependencyRepository.findByEventIdOrderByPrerequisiteEventIdAsc(event.getId()).stream()
                .map(RecoveryEventDependencyEntity::getPrerequisiteEventId)
                .toList();
        return new RecoveryEventDetails(event, auditRepository.findByEventIdOrderByOccurredAtAsc(event.getId()),
                attemptRepository.findByEventIdOrderByAttemptNumberAsc(event.getId()), prerequisiteEventIds);
    }

    public record Submission(RecoveryEventDetails details, boolean created) {
    }
}