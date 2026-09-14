package dev.shreemangalam.faultline.restore.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TerminalFailureScenario(
        UUID scenarioId,
        String correlationId,
        List<TerminalFailurePath> failurePaths,
        List<ScenarioRecoveryEvent> events,
        Instant loadedAt) {
}
