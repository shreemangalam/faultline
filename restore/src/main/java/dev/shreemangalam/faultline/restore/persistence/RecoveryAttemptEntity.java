package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restore_event_attempt")
public class RecoveryAttemptEntity {
    @Id
    private UUID id;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    @Column(name = "worker_id", nullable = false, length = 100)
    private String workerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecoveryAttemptOutcome outcome;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;

    protected RecoveryAttemptEntity() { }

    public RecoveryAttemptEntity(UUID id, UUID eventId, int attemptNumber, String workerId, Instant startedAt) {
        this.id = id;
        this.eventId = eventId;
        this.attemptNumber = attemptNumber;
        this.workerId = workerId;
        this.outcome = RecoveryAttemptOutcome.IN_PROGRESS;
        this.startedAt = startedAt;
    }

    public void finish(RecoveryAttemptOutcome outcome, String failureReason, Instant finishedAt) {
        if (this.outcome != RecoveryAttemptOutcome.IN_PROGRESS || this.finishedAt != null) {
            throw new IllegalStateException("Only an in-progress recovery attempt can be finished.");
        }
        if (outcome == RecoveryAttemptOutcome.IN_PROGRESS) {
            throw new IllegalArgumentException("A finished recovery attempt requires a terminal outcome.");
        }
        this.outcome = outcome;
        this.failureReason = failureReason;
        this.finishedAt = finishedAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getWorkerId() { return workerId; }
    public RecoveryAttemptOutcome getOutcome() { return outcome; }
    public String getFailureReason() { return failureReason; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
