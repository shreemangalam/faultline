package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.util.List;
import java.util.UUID;

public record ScenarioRecoveryEvent(
        String step,
        UUID eventId,
        String targetSystem,
        RecoveryEventState state,
        List<UUID> prerequisiteEventIds) {
}
