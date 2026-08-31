package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.List;
import java.util.Objects;

public record ImpactPriorityReport(SystemId failedSystemId, List<ImpactPriority> priorities) {

    public ImpactPriorityReport {
        Objects.requireNonNull(failedSystemId, "failed system ID must not be null");
        priorities = List.copyOf(priorities);
    }

    public record ImpactPriority(
            SystemId systemId,
            String label,
            int impactRing,
            int score,
            SeverityBand severity,
            ScoreBreakdown scoreBreakdown) {

        public ImpactPriority {
            Objects.requireNonNull(systemId, "system ID must not be null");
            Objects.requireNonNull(label, "system label must not be null");
            Objects.requireNonNull(severity, "severity must not be null");
            Objects.requireNonNull(scoreBreakdown, "score breakdown must not be null");
            if (impactRing < 1 || score < 0) {
                throw new IllegalArgumentException("impact ring must be positive and score must not be negative");
            }
        }
    }

    public record ScoreBreakdown(int criticalityPoints, int fanOutPoints, int slaPoints, int proximityPoints) {

        public int total() {
            return criticalityPoints + fanOutPoints + slaPoints + proximityPoints;
        }
    }
}
