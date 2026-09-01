package dev.shreemangalam.faultline.restore.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRecoveryEventRequest(@NotBlank @Size(max = 100) String workerId) {
}