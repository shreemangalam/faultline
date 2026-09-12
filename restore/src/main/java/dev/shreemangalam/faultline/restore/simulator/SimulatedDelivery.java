package dev.shreemangalam.faultline.restore.simulator;

import java.util.UUID;

public record SimulatedDelivery(
        int statusCode,
        boolean accepted,
        boolean duplicate,
        UUID receiptId,
        String outcome
) {
}
