package dev.shreemangalam.faultline.restore.execution;

public record DownstreamExecutionResult(
        DeliveryClassification classification,
        Integer httpStatus,
        long durationMillis,
        String reason
) {
}
