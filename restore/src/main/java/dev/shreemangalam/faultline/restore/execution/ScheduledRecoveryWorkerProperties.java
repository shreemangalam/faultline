package dev.shreemangalam.faultline.restore.execution;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "restore.worker")
public record ScheduledRecoveryWorkerProperties(
        @NotBlank @Size(max = 100) String id,
        @Min(1) @Max(10) int batchSize,
        @Min(30) @Max(300) int leaseSeconds
) {
}
