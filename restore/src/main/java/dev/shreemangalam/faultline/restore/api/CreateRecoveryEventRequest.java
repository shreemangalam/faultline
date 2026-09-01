package dev.shreemangalam.faultline.restore.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateRecoveryEventRequest(
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotBlank @Size(max = 100) String correlationId,
        @NotBlank @Size(max = 100) String targetSystem,
        @NotBlank @Size(max = 255) String payloadReference,
        @Min(1) @Max(5) int priority,
        Instant deadlineAt
) {
}