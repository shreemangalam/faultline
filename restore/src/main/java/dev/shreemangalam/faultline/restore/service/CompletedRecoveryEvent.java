package dev.shreemangalam.faultline.restore.service;

import java.util.List;
import java.util.UUID;

public record CompletedRecoveryEvent(UUID eventId, List<UUID> releasedEventIds) {
}