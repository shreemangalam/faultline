package dev.shreemangalam.faultline.restore.service;
import java.util.UUID;
public record RetryRecoveryEventCommand(UUID eventId, String workerId, String reason, Integer httpStatus, Long durationMillis) {
    public RetryRecoveryEventCommand(UUID eventId, String workerId, String reason) {
        this(eventId, workerId, reason, null, null);
    }
}