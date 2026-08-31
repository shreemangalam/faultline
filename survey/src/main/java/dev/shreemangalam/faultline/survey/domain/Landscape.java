package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Landscape(List<SystemNode> systems, List<IntegrationEdge> integrations) {

    public Landscape {
        systems = List.copyOf(Objects.requireNonNull(systems, "systems must not be null"));
        integrations = List.copyOf(Objects.requireNonNull(integrations, "integrations must not be null"));

        Set<SystemId> systemIds = new HashSet<>();
        for (SystemNode system : systems) {
            if (!systemIds.add(system.id())) {
                throw new IllegalArgumentException("duplicate system ID: " + system.id().value());
            }
        }

        Set<String> integrationIds = new HashSet<>();
        for (IntegrationEdge integration : integrations) {
            if (!integrationIds.add(integration.id())) {
                throw new IllegalArgumentException("duplicate integration ID: " + integration.id());
            }
            if (!systemIds.contains(integration.sourceSystemId()) || !systemIds.contains(integration.targetSystemId())) {
                throw new IllegalArgumentException("integration references an unknown system: " + integration.id());
            }
        }
    }

    public boolean containsSystem(SystemId systemId) {
        return systems.stream().anyMatch(system -> system.id().equals(systemId));
    }
}
