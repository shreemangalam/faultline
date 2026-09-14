package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;
import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

import java.time.Instant;
import java.util.Map;

public record RecoveryStatistics(
        Instant generatedAt,
        long totalEvents,
        long pendingEventCount,
        long terminalFailureCount,
        Map<RecoveryEventState, Long> eventCounts,
        long totalAttempts,
        Map<RecoveryAttemptOutcome, Long> attemptCounts,
        Long p95SuccessfulRecoveryLatencyMillis) {
}
