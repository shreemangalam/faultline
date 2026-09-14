package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RankedScenarioRecoveryEvent(
        String step,
        UUID eventId,
        String targetSystem,
        RecoveryEventState state,
        int priority,
        Instant deadlineAt,
        List<UUID> prerequisiteEventIds) {
}
