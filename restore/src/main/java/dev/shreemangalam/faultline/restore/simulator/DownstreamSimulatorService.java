package dev.shreemangalam.faultline.restore.simulator;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DownstreamSimulatorService {
    private final ConcurrentHashMap<String, TargetState> targets = new ConcurrentHashMap<>();

    public SimulatorTarget configure(String targetSystem, DownstreamBehavior behavior, long delayMillis) {
        TargetState state = new TargetState(behavior, delayMillis);
        targets.put(targetSystem, state);
        return state.snapshot(targetSystem);
    }

    public SimulatorTarget get(String targetSystem) {
        return state(targetSystem).snapshot(targetSystem);
    }

    public SimulatedDelivery deliver(String targetSystem, String idempotencyKey) {
        TargetState state = state(targetSystem);
        state.requestCount.incrementAndGet();
        if (state.behavior == DownstreamBehavior.DELAYED_SUCCESS) delay(state.delayMillis);
        return switch (state.behavior) {
            case SUCCESS, DELAYED_SUCCESS -> state.accept(targetSystem, idempotencyKey);
            case TEMPORARY_UNAVAILABLE -> new SimulatedDelivery(503, false, false, null, "TEMPORARY_UNAVAILABLE");
            case RATE_LIMITED -> new SimulatedDelivery(429, false, false, null, "RATE_LIMITED");
            case VALIDATION_FAILURE -> new SimulatedDelivery(422, false, false, null, "VALIDATION_FAILURE");
        };
    }

    private TargetState state(String targetSystem) {
        return targets.computeIfAbsent(targetSystem, ignored -> new TargetState(DownstreamBehavior.SUCCESS, 0));
    }

    private void delay(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulation was interrupted.", exception);
        }
    }

    private static final class TargetState {
        private final DownstreamBehavior behavior;
        private final long delayMillis;
        private final AtomicLong requestCount = new AtomicLong();
        private final AtomicLong acceptedCount = new AtomicLong();
        private final Set<String> acceptedKeys = ConcurrentHashMap.newKeySet();

        private TargetState(DownstreamBehavior behavior, long delayMillis) {
            this.behavior = behavior;
            this.delayMillis = delayMillis;
        }

        private SimulatedDelivery accept(String targetSystem, String idempotencyKey) {
            boolean duplicate = !acceptedKeys.add(idempotencyKey);
            if (!duplicate) acceptedCount.incrementAndGet();
            UUID receiptId = UUID.nameUUIDFromBytes((targetSystem + "\u001F" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
            return new SimulatedDelivery(200, true, duplicate, receiptId, duplicate ? "ALREADY_ACCEPTED" : "ACCEPTED");
        }

        private SimulatorTarget snapshot(String targetSystem) {
            return new SimulatorTarget(targetSystem, behavior, delayMillis, requestCount.get(), acceptedCount.get());
        }
    }
}
