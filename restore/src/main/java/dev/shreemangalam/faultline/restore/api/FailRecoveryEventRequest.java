package dev.shreemangalam.faultline.restore.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailRecoveryEventRequest(
        @NotBlank @Size(max = 100) String workerId,
        @NotBlank @Size(max = 255) String reason
) {
}
