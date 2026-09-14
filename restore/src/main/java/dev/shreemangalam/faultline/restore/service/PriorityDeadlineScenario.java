package dev.shreemangalam.faultline.restore.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PriorityDeadlineScenario(
        UUID scenarioId,
        String correlationId,
        List<RankedScenarioRecoveryEvent> events,
        List<String> expectedAdmissionOrder,
        Instant loadedAt) {
}
