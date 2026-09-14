package dev.shreemangalam.faultline.restore.service;

import java.util.List;
import java.util.UUID;

public record DependencyCascadeScenario(
        UUID scenarioId,
        String correlationId,
        List<ScenarioRecoveryEvent> events) {
}
