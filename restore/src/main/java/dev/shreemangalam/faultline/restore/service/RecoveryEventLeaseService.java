package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RecoveryEventLeaseService {
    private final RecoveryEventRepository eventRepository;
    private final RecoveryEventAuditRepository auditRepository;
    private final RecoveryEventDependencyRepository dependencyRepository;
    private final Clock clock;

    public RecoveryEventLeaseService(RecoveryEventRepository eventRepository, RecoveryEventAuditRepository auditRepository, RecoveryEventDependencyRepository dependencyRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
        this.dependencyRepository = dependencyRepository;
        this.clock = clock;
    }

    @Transactional
    public List<ClaimedRecoveryEvent> claim(ClaimRecoveryEventsCommand command) {
        Instant now = clock.instant();
        Instant leaseExpiresAt = now.plusSeconds(command.leaseSeconds());
        List<RecoveryEventEntity> events = eventRepository.lockClaimable(now, command.maxEvents());
        List<RecoveryEventAuditEntity> audits = events.stream().map(event -> {
            boolean reclaimed = event.getState() == RecoveryEventState.IN_PROGRESS;
            boolean retryDue = event.getState() == RecoveryEventState.RETRY_SCHEDULED;
            event.claim(command.workerId(), leaseExpiresAt, now);
            String reason = reclaimed ? "Visibility lease expired; reclaimed by worker " + command.workerId() + "."
                    : retryDue ? "Retry became eligible and was claimed by worker " + command.workerId() + "."
                    : "Claimed by worker " + command.workerId() + ".";
            return new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.IN_PROGRESS, reason, now);
        }).toList();
        auditRepository.saveAll(audits);
        return events.stream().map(event -> new ClaimedRecoveryEvent(event.getId(), event.getCorrelationId(), event.getTargetSystem(), event.getPayloadReference(), event.getPriority(), event.getAttemptCount(), event.getLeaseOwner(), event.getLeaseExpiresAt())).toList();
    }

    @Transactional
    public RetryRecoveryEvent retry(RetryRecoveryEventCommand command) {
        Instant now = clock.instant();
        RecoveryEventEntity event = ownedLease(command.eventId(), command.workerId(), now);
        if (event.getAttemptCount() >= 3) {
            event.exhaust(now);
            auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.EXHAUSTED, "Retry limit reached: " + command.reason(), now));
            return new RetryRecoveryEvent(event.getId(), event.getState(), null);
        }
        Instant nextEligibleAt = now.plus(retryDelay(event.getAttemptCount()));
        event.scheduleRetry(nextEligibleAt, now);
        auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.RETRY_SCHEDULED, command.reason(), now));
        return new RetryRecoveryEvent(event.getId(), event.getState(), nextEligibleAt);
    }

    @Transactional
    public CompletedRecoveryEvent complete(CompleteRecoveryEventCommand command) {
        Instant now = clock.instant();
        RecoveryEventEntity event = ownedLease(command.eventId(), command.workerId(), now);
        event.complete(now);
        auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.COMPLETED, "Completed by worker " + command.workerId() + ".", now));
        List<UUID> releasedEventIds = dependencyRepository.findByPrerequisiteEventIdOrderByEventIdAsc(event.getId()).stream().map(RecoveryEventDependencyEntity::getEventId).distinct().flatMap(id -> releaseIfUnblocked(id, now).stream()).toList();
        return new CompletedRecoveryEvent(event.getId(), releasedEventIds);
    }

    private RecoveryEventEntity ownedLease(UUID eventId, String workerId, Instant now) {
        RecoveryEventEntity event = eventRepository.lockById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery event was not found."));
        if (event.getState() != RecoveryEventState.IN_PROGRESS || !Objects.equals(event.getLeaseOwner(), workerId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Recovery event is not leased by this worker.");
        if (event.getLeaseExpiresAt() == null || !event.getLeaseExpiresAt().isAfter(now)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Recovery event lease has expired.");
        return event;
    }

    private Duration retryDelay(int attemptCount) { return Duration.ofSeconds(Math.min(60, 5L * (1L << Math.max(0, attemptCount - 1)))); }

    private List<UUID> releaseIfUnblocked(UUID eventId, Instant now) {
        RecoveryEventEntity dependent = eventRepository.lockById(eventId).orElseThrow();
        if (dependent.getState() != RecoveryEventState.BLOCKED || !allPrerequisitesComplete(eventId)) return List.of();
        dependent.release(now);
        auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), eventId, RecoveryEventState.ELIGIBLE, "All prerequisites completed.", now));
        return List.of(eventId);
    }

    private boolean allPrerequisitesComplete(UUID eventId) {
        List<RecoveryEventDependencyEntity> dependencies = dependencyRepository.findByEventIdOrderByPrerequisiteEventIdAsc(eventId);
        return !dependencies.isEmpty() && dependencies.stream().allMatch(dependency -> eventRepository.findById(dependency.getPrerequisiteEventId()).map(event -> event.getState() == RecoveryEventState.COMPLETED).orElse(false));
    }
}