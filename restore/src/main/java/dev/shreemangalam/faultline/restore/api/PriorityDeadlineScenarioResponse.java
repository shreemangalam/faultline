package dev.shreemangalam.faultline.restore.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PriorityDeadlineScenarioResponse(
        UUID scenarioId,
        String scenario,
        String correlationId,
        int eventCount,
        List<String> expectedAdmissionOrder,
        List<RankedScenarioRecoveryEventResponse> events,
        Instant loadedAt) {
}
