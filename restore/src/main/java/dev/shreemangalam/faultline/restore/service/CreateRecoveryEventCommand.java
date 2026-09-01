package dev.shreemangalam.faultline.restore.service;

import java.time.Instant;

public record CreateRecoveryEventCommand(String idempotencyKey, String correlationId, String targetSystem, String payloadReference, int priority, Instant deadlineAt) {
}