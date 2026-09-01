package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecoveryEventResponse(
        UUID eventId,
        String idempotencyKey,
        String correlationId,
        String targetSystem,
        String payloadReference,
        int priority,
        Instant deadlineAt,
        RecoveryEventState state,
        int attemptCount,
        Instant nextEligibleAt,
        String leaseOwner,
        Instant leaseExpiresAt,
        Instant createdAt,
        List<UUID> prerequisiteEventIds,
        List<RecoveryEventAuditResponse> audits,
        List<RecoveryAttemptResponse> attempts
) {
}