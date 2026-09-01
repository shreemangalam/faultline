package dev.shreemangalam.faultline.restore.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimRecoveryEventsRequest(
        @NotBlank @Size(max = 100) String workerId,
        @Min(1) @Max(50) int maxEvents,
        @Min(1) @Max(300) int leaseSeconds
) {
}