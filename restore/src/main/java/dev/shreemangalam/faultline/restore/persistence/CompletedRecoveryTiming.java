package dev.shreemangalam.faultline.restore.persistence;

import java.time.Instant;

public interface CompletedRecoveryTiming {
    Instant getCreatedAt();
    Instant getCompletedAt();
}
