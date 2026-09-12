package dev.shreemangalam.faultline.restore.service;

import java.util.UUID;

public record FailRecoveryEventCommand(UUID eventId, String workerId, String reason) {
}
