package dev.shreemangalam.faultline.restore.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateRecoveryEventCommand(
        String idempotencyKey,
        String correlationId,
        String targetSystem,
        String payloadReference,
        int priority,
        Instant deadlineAt,
        List<UUID> prerequisiteEventIds
) {
    public CreateRecoveryEventCommand {
        prerequisiteEventIds = prerequisiteEventIds == null ? List.of() : List.copyOf(prerequisiteEventIds);
    }
}