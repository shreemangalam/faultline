package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;
import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.persistence.RecoveryAttemptEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryAttemptRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventDependencyEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventDependencyRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventRepository;
import dev.shreemangalam.faultline.restore.streaming.RecoveryStateChangePublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoveryEventLeaseService {
    private final RecoveryEventRepository eventRepository;
    private final RecoveryEventAuditRepository auditRepository;
    private final RecoveryEventDependencyRepository dependencyRepository;
    private final RecoveryAttemptRepository attemptRepository;
    private final Clock clock;
    private final CircuitBreakerService circuitBreakerService;
    private final RetryBackoffPolicy retryBackoffPolicy;
    private final RecoveryStateChangePublisher stateChangePublisher;

    public RecoveryEventLeaseService(
            RecoveryEventRepository eventRepository,
            RecoveryEventAuditRepository auditRepository,
            RecoveryEventDependencyRepository dependencyRepository,
            RecoveryAttemptRepository attemptRepository,
            Clock clock,
            CircuitBreakerService circuitBreakerService,
            RetryBackoffPolicy retryBackoffPolicy,
            RecoveryStateChangePublisher stateChangePublisher) {
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
        this.dependencyRepository = dependencyRepository;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
        this.circuitBreakerService = circuitBreakerService;
        this.retryBackoffPolicy = retryBackoffPolicy;
        this.stateChangePublisher = stateChangePublisher;
    }

    @Transactional
    public List<ClaimedRecoveryEvent> claim(ClaimRecoveryEventsCommand command) {
        Instant now = clock.instant();
        Instant leaseExpiresAt = now.plusSeconds(command.leaseSeconds());
        List<RecoveryEventEntity> events = eventRepository.lockClaimable(now, command.maxEvents()).stream()
                .filter(event -> circuitBreakerService.allowClaim(event.getTargetSystem(), event.getId(), now, leaseExpiresAt))
                .toList();
        List<RecoveryEventAuditEntity> audits = events.stream().map(event -> {
            boolean reclaimed = event.getState() == RecoveryEventState.IN_PROGRESS;
            boolean retryDue = event.getState() == RecoveryEventState.RETRY_SCHEDULED;
            if (reclaimed) closeExpiredAttempt(event, now);
            event.claim(command.workerId(), leaseExpiresAt, now);
            stateChangePublisher.publish(event, now);
            attemptRepository.save(new RecoveryAttemptEntity(UUID.randomUUID(), event.getId(), event.getAttemptCount(), command.workerId(), now));
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
        RecoveryAttemptEntity attempt = activeAttempt(event);
        circuitBreakerService.recordFailure(event.getTargetSystem(), event.getId(), now);
        if (event.getAttemptCount() >= 3) {
            attempt.finish(RecoveryAttemptOutcome.EXHAUSTED, command.reason(), command.httpStatus(), command.durationMillis(), now);
            event.exhaust(now);
            auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.EXHAUSTED, "Retry limit reached: " + command.reason(), now));
            stateChangePublisher.publish(event, now);
            propagateDependencyFailure(event.getId(), event.getState(), now);
            return new RetryRecoveryEvent(event.getId(), event.getState(), null);
        }
        Instant nextEligibleAt = now.plus(retryBackoffPolicy.delayForAttempt(event.getAttemptCount()));
        attempt.finish(RecoveryAttemptOutcome.TRANSIENT_FAILURE, command.reason(), command.httpStatus(), command.durationMillis(), now);
        event.scheduleRetry(nextEligibleAt, now);
        auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.RETRY_SCHEDULED, command.reason(), now));
        stateChangePublisher.publish(event, now);
        return new RetryRecoveryEvent(event.getId(), event.getState(), nextEligibleAt);
    }

    @Transactional
    public FailedRecoveryEvent failFinal(FailRecoveryEventCommand command) {
        Instant now = clock.instant();
        RecoveryEventEntity event = ownedLease(command.eventId(), command.workerId(), now);
        activeAttempt(event).finish(RecoveryAttemptOutcome.PERMANENT_FAILURE, command.reason(), command.httpStatus(), command.durationMillis(), now);
        circuitBreakerService.recordPermanentFailure(event.getTargetSystem(), event.getId(), now);
        event.failFinal(now);
        auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.FAILED_FINAL, "Permanent failure: " + command.reason(), now));
        stateChangePublisher.publish(event, now);
        propagateDependencyFailure(event.getId(), event.getState(), now);
        return new FailedRecoveryEvent(event.getId(), event.getState());
    }

    @Transactional
    public CompletedRecoveryEvent complete(CompleteRecoveryEventCommand command) {
        Instant now = clock.instant();
        RecoveryEventEntity event = ownedLease(command.eventId(), command.workerId(), now);
        activeAttempt(event).finish(RecoveryAttemptOutcome.SUCCEEDED, null, command.httpStatus(), command.durationMillis(), now);
        circuitBreakerService.recordSuccess(event.getTargetSystem(), event.getId(), now);
        event.complete(now);
        auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.COMPLETED, "Completed by worker " + command.workerId() + ".", now));
        stateChangePublisher.publish(event, now);
        List<UUID> releasedEventIds = dependencyRepository.findByPrerequisiteEventIdOrderByEventIdAsc(event.getId()).stream().map(RecoveryEventDependencyEntity::getEventId).distinct().flatMap(id -> releaseIfUnblocked(id, now).stream()).toList();
        return new CompletedRecoveryEvent(event.getId(), releasedEventIds);
    }

    private RecoveryEventEntity ownedLease(UUID eventId, String workerId, Instant now) {
        RecoveryEventEntity event = eventRepository.lockById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery event was not found."));
        if (event.getState() != RecoveryEventState.IN_PROGRESS || !Objects.equals(event.getLeaseOwner(), workerId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Recovery event is not leased by this worker.");
        if (event.getLeaseExpiresAt() == null || !event.getLeaseExpiresAt().isAfter(now)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Recovery event lease has expired.");
        return event;
    }

    private RecoveryAttemptEntity activeAttempt(RecoveryEventEntity event) {
        return attemptRepository.findByEventIdAndFinishedAtIsNull(event.getId())
                .orElseGet(() -> attemptRepository.save(new RecoveryAttemptEntity(
                        UUID.randomUUID(), event.getId(), event.getAttemptCount(), event.getLeaseOwner(), event.getUpdatedAt())));
    }

    private void closeExpiredAttempt(RecoveryEventEntity event, Instant now) {
        activeAttempt(event).finish(RecoveryAttemptOutcome.LEASE_EXPIRED, "Visibility lease expired before completion.", now);
    }

    private void propagateDependencyFailure(
            UUID prerequisiteEventId,
            RecoveryEventState prerequisiteState,
            Instant now) {
        ArrayDeque<FailedPrerequisite> queue = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        queue.add(new FailedPrerequisite(prerequisiteEventId, prerequisiteState));

        while (!queue.isEmpty()) {
            FailedPrerequisite failed = queue.removeFirst();
            dependencyRepository.findByPrerequisiteEventIdOrderByEventIdAsc(failed.eventId()).forEach(dependency -> {
                UUID dependentId = dependency.getEventId();
                if (!visited.add(dependentId)) return;
                RecoveryEventEntity dependent = eventRepository.lockById(dependentId).orElseThrow();
                if (dependent.getState() != RecoveryEventState.BLOCKED) return;
                dependent.failDependency(now);
                stateChangePublisher.publish(dependent, now);
                auditRepository.save(new RecoveryEventAuditEntity(
                        UUID.randomUUID(),
                        dependentId,
                        RecoveryEventState.FAILED_DEPENDENCY,
                        "Prerequisite " + failed.eventId() + " reached " + failed.state()
                                + "; recovery cannot continue.",
                        now));
                queue.addLast(new FailedPrerequisite(dependentId, RecoveryEventState.FAILED_DEPENDENCY));
            });
        }
    }

    private List<UUID> releaseIfUnblocked(UUID eventId, Instant now) {
        RecoveryEventEntity dependent = eventRepository.lockById(eventId).orElseThrow();
        if (dependent.getState() != RecoveryEventState.BLOCKED || !allPrerequisitesComplete(eventId)) return List.of();
        dependent.release(now);
        stateChangePublisher.publish(dependent, now);
        auditRepository.save(new RecoveryEventAuditEntity(UUID.randomUUID(), eventId, RecoveryEventState.ELIGIBLE, "All prerequisites completed.", now));
        return List.of(eventId);
    }

    private record FailedPrerequisite(UUID eventId, RecoveryEventState state) {
    }
    private boolean allPrerequisitesComplete(UUID eventId) {
        List<RecoveryEventDependencyEntity> dependencies = dependencyRepository.findByEventIdOrderByPrerequisiteEventIdAsc(eventId);
        return !dependencies.isEmpty() && dependencies.stream().allMatch(dependency -> eventRepository.findById(dependency.getPrerequisiteEventId()).map(event -> event.getState() == RecoveryEventState.COMPLETED).orElse(false));
    }
}
