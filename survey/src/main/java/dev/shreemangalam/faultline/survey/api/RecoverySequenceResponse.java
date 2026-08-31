package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.RecoveryPlan;

import java.util.List;

public record RecoverySequenceResponse(
        String failedSystemId,
        List<String> recoverySequence,
        boolean cycleWarning,
        List<String> unresolvedSystemIds) {

    static RecoverySequenceResponse from(RecoveryPlan plan) {
        return new RecoverySequenceResponse(
                plan.failedSystemId().value(),
                plan.recoverySequence().stream().map(systemId -> systemId.value()).toList(),
                plan.hasCycleWarning(),
                plan.unresolvedSystemIds().stream().map(systemId -> systemId.value()).toList());
    }
}
