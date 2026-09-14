package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.simulator.DownstreamBehavior;

import java.time.Instant;
import java.util.UUID;

public record ThunderingHerdScenario(
        UUID scenarioId,
        String correlationId,
        String targetSystem,
        int eventCount,
        RecoveryEventState state,
        DownstreamBehavior downstreamBehavior,
        Instant loadedAt) {
}
