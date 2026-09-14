package dev.shreemangalam.faultline.restore.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TerminalFailureScenarioResponse(
        UUID scenarioId,
        String scenario,
        String correlationId,
        int eventCount,
        List<TerminalFailurePathResponse> failurePaths,
        List<ScenarioRecoveryEventResponse> events,
        Instant loadedAt) {
}
