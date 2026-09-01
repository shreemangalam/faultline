package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;

import java.time.Instant;
import java.util.UUID;

public record RecoveryAttemptResponse(
        UUID attemptId,
        int attemptNumber,
        String workerId,
        RecoveryAttemptOutcome outcome,
        String failureReason,
        Instant startedAt,
        Instant finishedAt
) { }
