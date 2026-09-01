package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.CircuitState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

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
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CircuitBreakerEntity() { }

    public CircuitBreakerEntity(String targetSystem, Instant now) {
        this.targetSystem = targetSystem;
        this.state = CircuitState.CLOSED;
        this.updatedAt = now;
    }

    public boolean allowClaim(Instant now) {
        if (state == CircuitState.CLOSED) return true;
        if (state == CircuitState.OPEN && openUntil != null && !openUntil.isAfter(now)) {
            state = CircuitState.HALF_OPEN;
            openUntil = null;
            updatedAt = now;
            return true;
        }
        return false;
    }

    public void recordFailure(Instant now) {
        consecutiveFailures++;
        updatedAt = now;
        if (state == CircuitState.HALF_OPEN) {
            state = CircuitState.OPEN;
            openUntil = now.plusSeconds(60);
        } else if (consecutiveFailures >= 3) {
            state = CircuitState.OPEN;
            openUntil = now.plusSeconds(30);
        }
    }

    public void recordSuccess(Instant now) {
        state = CircuitState.CLOSED;
        consecutiveFailures = 0;
        openUntil = null;
        updatedAt = now;
    }

    public String getTargetSystem() { return targetSystem; }
    public CircuitState getState() { return state; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public Instant getOpenUntil() { return openUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
}