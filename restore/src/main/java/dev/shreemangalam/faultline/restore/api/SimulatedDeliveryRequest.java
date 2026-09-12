package dev.shreemangalam.faultline.restore.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SimulatedDeliveryRequest(
        @NotNull UUID eventId,
        @NotBlank @Size(max = 100) String correlationId,
        @NotBlank @Size(max = 255) String payloadReference
) {
}
