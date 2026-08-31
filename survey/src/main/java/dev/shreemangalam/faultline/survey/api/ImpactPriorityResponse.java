package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.ImpactPriorityReport;

import java.util.List;

public record ImpactPriorityResponse(
        String failedSystemId,
        List<PrioritizedSystemResponse> priorities) {

    static ImpactPriorityResponse from(ImpactPriorityReport report) {
        return new ImpactPriorityResponse(
                report.failedSystemId().value(),
                report.priorities().stream().map(priority -> new PrioritizedSystemResponse(
                        priority.systemId().value(),
                        priority.label(),
                        priority.impactRing(),
                        priority.score(),
                        priority.severity().name(),
                        new ScoreBreakdownResponse(
                                priority.scoreBreakdown().criticalityPoints(),
                                priority.scoreBreakdown().fanOutPoints(),
                                priority.scoreBreakdown().slaPoints(),
                                priority.scoreBreakdown().proximityPoints())))
                        .toList());
    }

    public record PrioritizedSystemResponse(
            String systemId,
            String label,
            int impactRing,
            int score,
            String severity,
            ScoreBreakdownResponse scoreBreakdown) {
    }

    public record ScoreBreakdownResponse(
            int criticalityPoints,
            int fanOutPoints,
            int slaPoints,
            int proximityPoints) {
    }
}
