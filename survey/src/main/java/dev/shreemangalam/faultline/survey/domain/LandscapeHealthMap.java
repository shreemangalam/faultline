package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.HealthStatus;
import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.List;
import java.util.Objects;

public record LandscapeHealthMap(List<SystemHealth> systems, List<Integration> integrations) {

    public LandscapeHealthMap {
        systems = List.copyOf(systems);
        integrations = List.copyOf(integrations);
    }

    public record SystemHealth(SystemId systemId, String label, int criticality, HealthStatus health) {

        public SystemHealth {
            Objects.requireNonNull(systemId, "system ID must not be null");
            Objects.requireNonNull(label, "system label must not be null");
            Objects.requireNonNull(health, "health must not be null");
        }
    }

    public record Integration(String integrationId, SystemId sourceSystemId, SystemId targetSystemId, String protocol) {

        public Integration {
            Objects.requireNonNull(integrationId, "integration ID must not be null");
            Objects.requireNonNull(sourceSystemId, "source system ID must not be null");
            Objects.requireNonNull(targetSystemId, "target system ID must not be null");
            Objects.requireNonNull(protocol, "protocol must not be null");
        }
    }
}
