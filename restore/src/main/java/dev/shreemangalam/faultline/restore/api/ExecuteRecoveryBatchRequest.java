package dev.shreemangalam.faultline.restore.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExecuteRecoveryBatchRequest(
        @NotBlank @Size(max = 100) String workerId,
        @Min(1) @Max(10) int maxEvents,
        @Min(30) @Max(300) int leaseSeconds
) {
}
