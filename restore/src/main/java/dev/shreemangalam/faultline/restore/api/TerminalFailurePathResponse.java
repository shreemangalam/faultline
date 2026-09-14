package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.simulator.DownstreamBehavior;

public record TerminalFailurePathResponse(
        String path,
        String targetSystem,
        DownstreamBehavior downstreamBehavior,
        RecoveryEventState expectedTerminalState,
        int expectedAttempts) {
}
