package dev.shreemangalam.faultline.restore.service;

import java.util.UUID;

public record FailRecoveryEventCommand(UUID eventId, String workerId, String reason, Integer httpStatus, Long durationMillis) {
    public FailRecoveryEventCommand(UUID eventId, String workerId, String reason) {
        this(eventId, workerId, reason, null, null);
    }
}
