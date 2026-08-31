package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.LandscapeHealthMap;

import java.util.List;

public record LandscapeHealthMapResponse(List<SystemResponse> systems, List<IntegrationResponse> integrations) {
    static LandscapeHealthMapResponse from(LandscapeHealthMap healthMap) {
        return new LandscapeHealthMapResponse(
                healthMap.systems().stream().map(system -> new SystemResponse(system.systemId().value(), system.label(), system.criticality(), system.health().name())).toList(),
                healthMap.integrations().stream().map(integration -> new IntegrationResponse(integration.integrationId(), integration.sourceSystemId().value(), integration.targetSystemId().value(), integration.protocol(), integration.dataDomain(), integration.slaClass().name())).toList());
    }
    public record SystemResponse(String systemId, String label, int criticality, String health) { }
    public record IntegrationResponse(String integrationId, String sourceSystemId, String targetSystemId, String protocol, String dataDomain, String slaClass) { }
}