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
@Table(name = "restore_event_audit")
public class RecoveryEventAuditEntity {
    @Id
    private UUID id;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 32)
    private RecoveryEventState toState;
    @Column(nullable = false, length = 255)
    private String reason;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected RecoveryEventAuditEntity() { }

    public RecoveryEventAuditEntity(UUID id, UUID eventId, RecoveryEventState toState, String reason, Instant occurredAt) {
        this.id = id;
        this.eventId = eventId;
        this.toState = toState;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public RecoveryEventState getToState() { return toState; }
    public String getReason() { return reason; }
    public Instant getOccurredAt() { return occurredAt; }
}