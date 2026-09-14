package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;

public interface RecoveryAttemptOutcomeCount {
    RecoveryAttemptOutcome getOutcome();
    long getCount();
}
