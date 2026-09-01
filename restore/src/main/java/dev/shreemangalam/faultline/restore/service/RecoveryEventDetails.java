package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.persistence.RecoveryAttemptEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventEntity;

import java.util.List;
import java.util.UUID;

public record RecoveryEventDetails(RecoveryEventEntity event, List<RecoveryEventAuditEntity> audits, List<RecoveryAttemptEntity> attempts, List<UUID> prerequisiteEventIds) {
}