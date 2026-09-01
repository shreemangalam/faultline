package dev.shreemangalam.faultline.restore.service;

import java.util.UUID;

public record CompleteRecoveryEventCommand(UUID eventId, String workerId) {
}