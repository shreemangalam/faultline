package dev.shreemangalam.faultline.restore.service;

import java.time.Instant;
import java.util.UUID;

public record ClaimedRecoveryEvent(
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