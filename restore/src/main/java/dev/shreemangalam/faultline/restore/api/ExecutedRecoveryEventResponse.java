package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.execution.DeliveryClassification;

import java.util.List;
import java.util.UUID;

public record ExecutedRecoveryEventResponse(
        UUID eventId,
        RecoveryEventState state,
        DeliveryClassification classification,
        Integer httpStatus,
        long durationMillis,
        String reason,
        List<UUID> releasedEventIds
) {
}
