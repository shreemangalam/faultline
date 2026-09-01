package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.persistence.CircuitBreakerEntity;
import dev.shreemangalam.faultline.restore.persistence.CircuitBreakerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

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
    public boolean allowClaim(String targetSystem, Instant now) {
        return repository.lockByTargetSystem(targetSystem).map(circuit -> circuit.allowClaim(now)).orElse(true);
    }

    @Transactional
    public void recordFailure(String targetSystem, Instant now) {
        CircuitBreakerEntity circuit = repository.lockByTargetSystem(targetSystem)
                .orElseGet(() -> repository.save(new CircuitBreakerEntity(targetSystem, now)));
        circuit.recordFailure(now);
    }

    @Transactional
    public void recordSuccess(String targetSystem, Instant now) {
        repository.lockByTargetSystem(targetSystem).ifPresent(circuit -> circuit.recordSuccess(now));
    }

    @Transactional(readOnly = true)
    public List<CircuitBreakerDetails> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(CircuitBreakerEntity::getTargetSystem))
                .map(circuit -> new CircuitBreakerDetails(circuit.getTargetSystem(), circuit.getState(), circuit.getConsecutiveFailures(), circuit.getOpenUntil(), circuit.getUpdatedAt()))
                .toList();
    }
}