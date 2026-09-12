package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.util.UUID;

public record FailedRecoveryEventResponse(UUID eventId, RecoveryEventState state) {
}
