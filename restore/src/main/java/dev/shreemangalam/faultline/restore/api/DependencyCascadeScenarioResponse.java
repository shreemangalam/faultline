package dev.shreemangalam.faultline.restore.api;

import java.util.List;
import java.util.UUID;

public record DependencyCascadeScenarioResponse(
        UUID scenarioId,
        String scenario,
        String correlationId,
        int eventCount,
        List<ScenarioRecoveryEventResponse> events) {
}
