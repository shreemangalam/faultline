package dev.shreemangalam.faultline.restore.simulator;

public record SimulatorTarget(
        String targetSystem,
        DownstreamBehavior behavior,
        long delayMillis,
        long requestCount,
        long acceptedCount
) {
}
