package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.List;
import java.util.Objects;

public record ImpactAnalysis(
        SystemId failedSystemId,
        List<AffectedSystem> affectedSystems,
        List<AffectedIntegration> affectedIntegrations) {

    public ImpactAnalysis {
        Objects.requireNonNull(failedSystemId, "failed system ID must not be null");
        affectedSystems = List.copyOf(affectedSystems);
        affectedIntegrations = List.copyOf(affectedIntegrations);
    }

    public record AffectedSystem(SystemId systemId, String label, int impactRing) {

        public AffectedSystem {
            Objects.requireNonNull(systemId, "system ID must not be null");
            Objects.requireNonNull(label, "system label must not be null");
            if (impactRing < 1) {
                throw new IllegalArgumentException("affected system impact ring must be positive");
            }
        }
    }

    public record AffectedIntegration(
            String integrationId,
            SystemId sourceSystemId,
            SystemId targetSystemId,
            String protocol,
            int impactRing) {

        public AffectedIntegration {
            Objects.requireNonNull(integrationId, "integration ID must not be null");
            Objects.requireNonNull(sourceSystemId, "source system ID must not be null");
            Objects.requireNonNull(targetSystemId, "target system ID must not be null");
            Objects.requireNonNull(protocol, "protocol must not be null");
            if (impactRing < 1) {
                throw new IllegalArgumentException("affected integration impact ring must be positive");
            }
        }
    }
}
