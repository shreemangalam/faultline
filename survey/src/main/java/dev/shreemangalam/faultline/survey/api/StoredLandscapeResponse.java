package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.service.StoredLandscape;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoredLandscapeResponse(UUID importId, Instant createdAt, List<SystemResponse> systems, List<IntegrationResponse> integrations) {
    static StoredLandscapeResponse from(StoredLandscape landscape) {
        return new StoredLandscapeResponse(landscape.importId(), landscape.createdAt(),
                landscape.systems().stream().map(system -> new SystemResponse(system.systemId(), system.label(), system.criticality())).toList(),
                landscape.integrations().stream().map(integration -> new IntegrationResponse(integration.integrationId(), integration.sourceSystemId(), integration.targetSystemId(), integration.protocol(), integration.dataDomain(), integration.slaClass())).toList());
    }
    public record SystemResponse(String systemId, String label, int criticality) { }
    public record IntegrationResponse(String integrationId, String sourceSystemId, String targetSystemId, String protocol, String dataDomain, String slaClass) { }
}