package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventEntity;

import java.util.List;

public record RecoveryEventDetails(RecoveryEventEntity event, List<RecoveryEventAuditEntity> audits) {
}