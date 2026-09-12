package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.simulator.DownstreamBehavior;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigureSimulatorTargetRequest(
        @NotNull DownstreamBehavior behavior,
        @Min(0) @Max(10_000) long delayMillis
) {
}
