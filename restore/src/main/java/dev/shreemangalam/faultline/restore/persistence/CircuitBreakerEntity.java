package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.CircuitState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restore_circuit_breaker")
public class CircuitBreakerEntity {
    @Id
    @Column(name = "target_system", length = 100)
    private String targetSystem;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CircuitState state;
    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;
    @Column(name = "open_until")
    private Instant openUntil;
    @Column(name = "probe_event_id")
    private UUID probeEventId;
    @Column(name = "probe_lease_expires_at")
    private Instant probeLeaseExpiresAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CircuitBreakerEntity() { }

    public CircuitBreakerEntity(String targetSystem, Instant now) {
        this.targetSystem = targetSystem;
        this.state = CircuitState.CLOSED;
        this.updatedAt = now;
    }

    public boolean allowClaim(UUID eventId, Instant now, Instant leaseExpiresAt) {
        if (state == CircuitState.CLOSED) return true;
        boolean cooldownElapsed = state == CircuitState.OPEN && openUntil != null && !openUntil.isAfter(now);
        boolean probeExpired = state == CircuitState.HALF_OPEN
                && (probeLeaseExpiresAt == null || !probeLeaseExpiresAt.isAfter(now));
        if (cooldownElapsed || probeExpired) {
            state = CircuitState.HALF_OPEN;
            openUntil = null;
            probeEventId = eventId;
            probeLeaseExpiresAt = leaseExpiresAt;
            updatedAt = now;
            return true;
        }
        return false;
    }

    public void recordFailure(UUID eventId, Instant now) {
        if (state == CircuitState.HALF_OPEN && !eventId.equals(probeEventId)) return;
        consecutiveFailures++;
        updatedAt = now;
        if (state == CircuitState.HALF_OPEN) {
            state = CircuitState.OPEN;
            openUntil = now.plusSeconds(60);
        } else if (consecutiveFailures >= 3) {
            state = CircuitState.OPEN;
            openUntil = now.plusSeconds(30);
        }
        clearProbe();
    }

    public void recordSuccess(UUID eventId, Instant now) {
        if (state == CircuitState.OPEN || (state == CircuitState.HALF_OPEN && !eventId.equals(probeEventId))) return;
        close(now);
    }

    public void recordPermanentFailure(UUID eventId, Instant now) {
        // A reachable target's rejection settles its probe without retrying invalid input.
        if (state == CircuitState.HALF_OPEN && eventId.equals(probeEventId)) close(now);
    }

    private void close(Instant now) {
        state = CircuitState.CLOSED;
        consecutiveFailures = 0;
        openUntil = null;
        updatedAt = now;
        clearProbe();
    }

    private void clearProbe() {
        probeEventId = null;
        probeLeaseExpiresAt = null;
    }

    public String getTargetSystem() { return targetSystem; }
    public CircuitState getState() { return state; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public Instant getOpenUntil() { return openUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
}
