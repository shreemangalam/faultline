package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.time.Instant;
import java.util.UUID;

public record RecoveryEventAuditResponse(UUID auditId, RecoveryEventState toState, String reason, Instant occurredAt) {
}