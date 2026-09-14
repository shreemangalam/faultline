package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;

public interface RecoveryEventStateCount {
    RecoveryEventState getState();
    long getCount();
}
