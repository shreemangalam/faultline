package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.List;
import java.util.Objects;

public record RecoveryPlan(
        SystemId failedSystemId,
        List<SystemId> recoverySequence,
        List<SystemId> unresolvedSystemIds) {

    public RecoveryPlan {
        Objects.requireNonNull(failedSystemId, "failed system ID must not be null");
        recoverySequence = List.copyOf(recoverySequence);
        unresolvedSystemIds = List.copyOf(unresolvedSystemIds);
    }

    public boolean hasCycleWarning() {
        return !unresolvedSystemIds.isEmpty();
    }
}
