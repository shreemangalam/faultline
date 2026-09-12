package dev.shreemangalam.faultline.restore.service;

import java.util.UUID;

public record CompleteRecoveryEventCommand(UUID eventId, String workerId, Integer httpStatus, Long durationMillis) {
    public CompleteRecoveryEventCommand(UUID eventId, String workerId) {
        this(eventId, workerId, null, null);
    }
}