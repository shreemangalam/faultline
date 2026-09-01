package dev.shreemangalam.faultline.restore.domain;

public enum RecoveryAttemptOutcome {
    IN_PROGRESS,
    SUCCEEDED,
    TRANSIENT_FAILURE,
    LEASE_EXPIRED,
    EXHAUSTED,
    PERMANENT_FAILURE
}
