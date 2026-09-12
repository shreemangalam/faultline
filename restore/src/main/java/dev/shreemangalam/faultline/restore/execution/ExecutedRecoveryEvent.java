package dev.shreemangalam.faultline.restore.execution;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.util.List;
import java.util.UUID;

public record ExecutedRecoveryEvent(
        UUID eventId,
        RecoveryEventState state,
        DeliveryClassification classification,
        Integer httpStatus,
        long durationMillis,
        String reason,
        List<UUID> releasedEventIds
) {
}
