package dev.shreemangalam.faultline.restore.streaming;

import dev.shreemangalam.faultline.restore.api.RestoreStreamReadyResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecoveryEventStream {
    private final ConcurrentHashMap<UUID, SseEmitter> subscribers = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("restore-sse-heartbeat").factory());
    private final Clock clock;
    private final Duration timeout;
    private final Duration heartbeatInterval;
    private final RecoveryNotificationDispatcher notificationDispatcher;

    public RecoveryEventStream(
            Clock clock,
            @Value("${restore.stream.timeout:30m}") Duration timeout,
            @Value("${restore.stream.heartbeat-interval:15s}") Duration heartbeatInterval,
            RecoveryNotificationDispatcher notificationDispatcher) {
        if (timeout.toMillis() <= 0) throw new IllegalArgumentException("Stream timeout must be positive.");
        if (heartbeatInterval.toMillis() <= 0) {
            throw new IllegalArgumentException("Stream heartbeat interval must be positive.");
        }
        this.clock = clock;
        this.timeout = timeout;
        this.heartbeatInterval = heartbeatInterval;
        this.notificationDispatcher = notificationDispatcher;
    }

    @PostConstruct
    void startHeartbeat() {
        heartbeatExecutor.scheduleWithFixedDelay(
                this::heartbeat,
                heartbeatInterval.toMillis(),
                heartbeatInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stopHeartbeat() {
        heartbeatExecutor.shutdownNow();
        subscribers.forEach((subscriberId, emitter) -> {
            if (subscribers.remove(subscriberId, emitter)) emitter.complete();
        });
    }

    public SseEmitter subscribe() {
        UUID subscriberId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(timeout.toMillis());
        subscribers.put(subscriberId, emitter);
        emitter.onCompletion(() -> subscribers.remove(subscriberId, emitter));
        emitter.onTimeout(() -> subscribers.remove(subscriberId, emitter));
        emitter.onError(ignored -> subscribers.remove(subscriberId, emitter));
        send(subscriberId, emitter, SseEmitter.event()
                .id(Long.toString(sequence.incrementAndGet()))
                .name("stream-ready")
                .data(new RestoreStreamReadyResponse("CONNECTED", clock.instant())));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcast(RecoveryStateChange change) {
        notificationDispatcher.dispatch(() -> {
            long eventId = sequence.incrementAndGet();
            subscribers.forEach((subscriberId, emitter) -> send(
                    subscriberId,
                    emitter,
                    SseEmitter.event()
                            .id(Long.toString(eventId))
                            .name("recovery-event")
                            .data(change)));
        });
    }

    private void heartbeat() {
        subscribers.forEach((subscriberId, emitter) -> send(
                subscriberId,
                emitter,
                SseEmitter.event().comment("heartbeat")));
    }

    private void send(UUID subscriberId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ignored) {
            subscribers.remove(subscriberId, emitter);
        }
    }
}
