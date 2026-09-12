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

    public SimulatorTarget configure(String targetSystem, DownstreamBehavior behavior, long delayMillis, boolean resetHistory) {
        TargetState state;
        if (resetHistory) {
            state = new TargetState(behavior, delayMillis);
            targets.put(targetSystem, state);
        } else {
            state = state(targetSystem);
            state.configure(behavior, delayMillis);
        }
        return state.snapshot(targetSystem);
    }

    public SimulatorTarget get(String targetSystem) {
        return state(targetSystem).snapshot(targetSystem);
    }

    public SimulatedDelivery deliver(String targetSystem, String idempotencyKey) {
        TargetState state = state(targetSystem);
        TargetConfiguration configuration = state.configuration();
        state.requestCount.incrementAndGet();
        if (configuration.behavior() == DownstreamBehavior.DELAYED_SUCCESS) delay(configuration.delayMillis());
        return switch (configuration.behavior()) {
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
        private volatile TargetConfiguration configuration;
        private final AtomicLong requestCount = new AtomicLong();
        private final AtomicLong acceptedCount = new AtomicLong();
        private final Set<String> acceptedKeys = ConcurrentHashMap.newKeySet();

        private TargetState(DownstreamBehavior behavior, long delayMillis) {
            configuration = new TargetConfiguration(behavior, delayMillis);
        }

        private void configure(DownstreamBehavior behavior, long delayMillis) {
            configuration = new TargetConfiguration(behavior, delayMillis);
        }

        private TargetConfiguration configuration() {
            return configuration;
        }

        private SimulatedDelivery accept(String targetSystem, String idempotencyKey) {
            boolean duplicate = !acceptedKeys.add(idempotencyKey);
            if (!duplicate) acceptedCount.incrementAndGet();
            UUID receiptId = UUID.nameUUIDFromBytes((targetSystem + "\u001F" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
            return new SimulatedDelivery(200, true, duplicate, receiptId, duplicate ? "ALREADY_ACCEPTED" : "ACCEPTED");
        }

        private SimulatorTarget snapshot(String targetSystem) {
            TargetConfiguration snapshot = configuration;
            return new SimulatorTarget(
                    targetSystem, snapshot.behavior(), snapshot.delayMillis(), requestCount.get(), acceptedCount.get());
        }
    }

    private record TargetConfiguration(DownstreamBehavior behavior, long delayMillis) {
    }
}
