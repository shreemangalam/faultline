package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.simulator.DownstreamBehavior;

public record TerminalFailurePath(
        String path,
        String targetSystem,
        DownstreamBehavior downstreamBehavior,
        RecoveryEventState expectedTerminalState,
        int expectedAttempts) {
}
