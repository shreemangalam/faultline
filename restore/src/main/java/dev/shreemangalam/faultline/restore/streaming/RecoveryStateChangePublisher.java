package dev.shreemangalam.faultline.restore.streaming;

import dev.shreemangalam.faultline.restore.persistence.RecoveryEventEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RecoveryStateChangePublisher {
    private final ApplicationEventPublisher publisher;

    public RecoveryStateChangePublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(RecoveryEventEntity event, Instant occurredAt) {
        publisher.publishEvent(new RecoveryStateChange(
                event.getId(),
                event.getCorrelationId(),
                event.getTargetSystem(),
                event.getState(),
                occurredAt));
    }
}
