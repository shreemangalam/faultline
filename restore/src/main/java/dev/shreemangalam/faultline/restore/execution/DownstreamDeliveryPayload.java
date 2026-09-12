package dev.shreemangalam.faultline.restore.execution;

import java.util.UUID;

record DownstreamDeliveryPayload(UUID eventId, String correlationId, String payloadReference) {
}
