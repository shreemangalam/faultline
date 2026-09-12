package dev.shreemangalam.faultline.restore.domain;

public enum RecoveryEventState {
    RECEIVED,
    ELIGIBLE,
    BLOCKED,
    IN_PROGRESS,
    RETRY_SCHEDULED,
    COMPLETED,
    EXHAUSTED,
    FAILED_FINAL,
    FAILED_DEPENDENCY
}