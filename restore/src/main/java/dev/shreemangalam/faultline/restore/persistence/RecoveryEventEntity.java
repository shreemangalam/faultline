package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restore_event")
public class RecoveryEventEntity {
    @Id
    private UUID id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    @Column(name = "request_fingerprint", nullable = false, length = 512)
    private String requestFingerprint;
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    @Column(name = "target_system", nullable = false, length = 100)
    private String targetSystem;
    @Column(name = "payload_reference", nullable = false, length = 255)
    private String payloadReference;
    @Column(nullable = false)
    private int priority;
    @Column(name = "deadline_at")
    private Instant deadlineAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecoveryEventState state;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "next_eligible_at", nullable = false)
    private Instant nextEligibleAt;
    @Column(name = "lease_owner", length = 100)
    private String leaseOwner;
    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecoveryEventEntity() { }

    private RecoveryEventEntity(UUID id, String idempotencyKey, String requestFingerprint, String correlationId, String targetSystem, String payloadReference, int priority, Instant deadlineAt, RecoveryEventState initialState, Instant now) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.correlationId = correlationId;
        this.targetSystem = targetSystem;
        this.payloadReference = payloadReference;
        this.priority = priority;
        this.deadlineAt = deadlineAt;
        this.state = initialState;
        this.attemptCount = 0;
        this.nextEligibleAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static RecoveryEventEntity receive(UUID id, String idempotencyKey, String requestFingerprint, String correlationId, String targetSystem, String payloadReference, int priority, Instant deadlineAt, RecoveryEventState initialState, Instant now) {
        return new RecoveryEventEntity(id, idempotencyKey, requestFingerprint, correlationId, targetSystem, payloadReference, priority, deadlineAt, initialState, now);
    }

    public void claim(String workerId, Instant expiresAt, Instant now) {
        if (state != RecoveryEventState.ELIGIBLE && state != RecoveryEventState.IN_PROGRESS) {
            throw new IllegalStateException("Only eligible or leased events can be claimed.");
        }
        state = RecoveryEventState.IN_PROGRESS;
        leaseOwner = workerId;
        leaseExpiresAt = expiresAt;
        attemptCount++;
        updatedAt = now;
    }

    public void complete(Instant now) {
        if (state != RecoveryEventState.IN_PROGRESS) {
            throw new IllegalStateException("Only leased events can be completed.");
        }
        state = RecoveryEventState.COMPLETED;
        leaseOwner = null;
        leaseExpiresAt = null;
        updatedAt = now;
    }

    public void release(Instant now) {
        if (state != RecoveryEventState.BLOCKED) {
            throw new IllegalStateException("Only blocked events can be released.");
        }
        state = RecoveryEventState.ELIGIBLE;
        nextEligibleAt = now;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public String getCorrelationId() { return correlationId; }
    public String getTargetSystem() { return targetSystem; }
    public String getPayloadReference() { return payloadReference; }
    public int getPriority() { return priority; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public RecoveryEventState getState() { return state; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextEligibleAt() { return nextEligibleAt; }
    public String getLeaseOwner() { return leaseOwner; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}