package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.HealthStatus;
import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.List;
import java.util.Objects;

public record LandscapeHealthMap(List<SystemHealth> systems, List<Integration> integrations) {
    public LandscapeHealthMap { systems = List.copyOf(systems); integrations = List.copyOf(integrations); }
    public record SystemHealth(SystemId systemId, String label, int criticality, HealthStatus health) {
        public SystemHealth { Objects.requireNonNull(systemId); Objects.requireNonNull(label); Objects.requireNonNull(health); }
    }
    public record Integration(String integrationId, SystemId sourceSystemId, SystemId targetSystemId, String protocol, String dataDomain, SlaClass slaClass) {
        public Integration { Objects.requireNonNull(integrationId); Objects.requireNonNull(sourceSystemId); Objects.requireNonNull(targetSystemId); Objects.requireNonNull(protocol); Objects.requireNonNull(dataDomain); Objects.requireNonNull(slaClass); }
    }
}