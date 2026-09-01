package dev.shreemangalam.faultline.restore.api;

import java.time.Instant;
import java.util.UUID;

public record ClaimedRecoveryEventResponse(
        UUID eventId,
        String correlationId,
        String targetSystem,
        String payloadReference,
        int priority,
        int attemptCount,
        String leaseOwner,
        Instant leaseExpiresAt
) {
}