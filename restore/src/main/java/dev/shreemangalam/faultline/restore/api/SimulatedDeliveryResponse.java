package dev.shreemangalam.faultline.restore.api;

import java.util.UUID;

public record SimulatedDeliveryResponse(
        boolean accepted,
        boolean duplicate,
        UUID receiptId,
        String outcome
) {
}
