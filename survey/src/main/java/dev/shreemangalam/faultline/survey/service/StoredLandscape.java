package dev.shreemangalam.faultline.survey.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoredLandscape(
        UUID importId,
        Instant createdAt,
        List<SystemSnapshot> systems,
        List<IntegrationSnapshot> integrations) {

    public StoredLandscape {
        systems = List.copyOf(systems);
        integrations = List.copyOf(integrations);
    }

    public record SystemSnapshot(String systemId, String label, int criticality) {
    }

    public record IntegrationSnapshot(
            String integrationId,
            String sourceSystemId,
            String targetSystemId,
            String protocol) {
    }
}
