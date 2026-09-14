package dev.shreemangalam.faultline.restore.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ThunderingHerdScenarioRequest(
        @Min(1) @Max(50_000) Integer eventCount) {
    public ThunderingHerdScenarioRequest {
        eventCount = eventCount == null ? 100 : eventCount;
    }
}
