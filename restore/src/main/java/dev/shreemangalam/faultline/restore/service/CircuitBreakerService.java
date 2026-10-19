package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.persistence.CircuitBreakerEntity;
import dev.shreemangalam.faultline.restore.persistence.CircuitBreakerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CircuitBreakerService {
    private final CircuitBreakerRepository repository;

    public CircuitBreakerService(CircuitBreakerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void ensureExists(String targetSystem, Instant now) {
        if (!repository.existsById(targetSystem)) repository.save(new CircuitBreakerEntity(targetSystem, now));
    }

    @Transactional
    public boolean allowClaim(String targetSystem, UUID eventId, Instant now, Instant leaseExpiresAt) {
        return repository.lockByTargetSystem(targetSystem)
                .map(circuit -> circuit.allowClaim(eventId, now, leaseExpiresAt)).orElse(true);
    }

    @Transactional
    public void recordFailure(String targetSystem, UUID eventId, Instant now) {
        CircuitBreakerEntity circuit = repository.lockByTargetSystem(targetSystem)
                .orElseGet(() -> repository.save(new CircuitBreakerEntity(targetSystem, now)));
        circuit.recordFailure(eventId, now);
    }

    @Transactional
    public void recordSuccess(String targetSystem, UUID eventId, Instant now) {
        repository.lockByTargetSystem(targetSystem).ifPresent(circuit -> circuit.recordSuccess(eventId, now));
    }

    @Transactional
    public void recordPermanentFailure(String targetSystem, UUID eventId, Instant now) {
        repository.lockByTargetSystem(targetSystem).ifPresent(circuit -> circuit.recordPermanentFailure(eventId, now));
    }

    @Transactional(readOnly = true)
    public List<CircuitBreakerDetails> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(CircuitBreakerEntity::getTargetSystem))
                .map(circuit -> new CircuitBreakerDetails(circuit.getTargetSystem(), circuit.getState(), circuit.getConsecutiveFailures(), circuit.getOpenUntil(), circuit.getUpdatedAt()))
                .toList();
    }
}
