package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditRepository;
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
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RecoveryEventService {

    private final RecoveryEventRepository eventRepository;
    private final RecoveryEventAuditRepository auditRepository;
    private final Clock clock;

    public RecoveryEventService(RecoveryEventRepository eventRepository, RecoveryEventAuditRepository auditRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    public Submission submit(CreateRecoveryEventCommand command) {
        String fingerprint = fingerprint(command);
        var existing = eventRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key is already associated with a different event.");
            }
            return new Submission(details(existing.get()), false);
        }

        Instant now = clock.instant();
        RecoveryEventEntity event = RecoveryEventEntity.receive(UUID.randomUUID(), command.idempotencyKey(), fingerprint, command.correlationId(), command.targetSystem(), command.payloadReference(), command.priority(), command.deadlineAt(), now);
        eventRepository.save(event);
        auditRepository.saveAll(List.of(
                new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.RECEIVED, "Event accepted for recovery.", now),
                new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.ELIGIBLE, "No prerequisites are registered.", now)
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

    private RecoveryEventDetails details(RecoveryEventEntity event) {
        return new RecoveryEventDetails(event, auditRepository.findByEventIdOrderByOccurredAtAsc(event.getId()));
    }

    private String fingerprint(CreateRecoveryEventCommand command) {
        String canonical = String.join("\u001F", command.correlationId(), command.targetSystem(), command.payloadReference(), Integer.toString(command.priority()), command.deadlineAt() == null ? "" : command.deadlineAt().toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the JVM.", exception);
        }
    }

    public record Submission(RecoveryEventDetails details, boolean created) {
    }
}