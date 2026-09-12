package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.util.UUID;

public record FailedRecoveryEvent(UUID eventId, RecoveryEventState state) {
}
