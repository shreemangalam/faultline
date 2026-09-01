package dev.shreemangalam.faultline.restore.api;

import java.util.List;
import java.util.UUID;

public record CompletedRecoveryEventResponse(UUID eventId, List<UUID> releasedEventIds) {
}