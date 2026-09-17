package dev.shreemangalam.faultline.restore.streaming;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.time.Instant;
import java.util.UUID;

public record RecoveryStateChange(
        UUID eventId,
        String correlationId,
        String targetSystem,
        RecoveryEventState state,
        Instant occurredAt) {
}
