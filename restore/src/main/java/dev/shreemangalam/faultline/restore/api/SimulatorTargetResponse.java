package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.simulator.DownstreamBehavior;

public record SimulatorTargetResponse(
        String targetSystem,
        DownstreamBehavior behavior,
        long delayMillis,
        long requestCount,
        long acceptedCount
) {
}
