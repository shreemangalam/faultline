package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.ImpactAnalysis;

import java.util.List;

public record ImpactAnalysisResponse(IncidentResponse incident, List<AffectedSystemResponse> affectedSystems, List<AffectedIntegrationResponse> affectedIntegrations) {
    static ImpactAnalysisResponse from(ImpactAnalysis analysis, DegradationType degradationType) {
        return new ImpactAnalysisResponse(new IncidentResponse(analysis.failedSystemId().value(), degradationType),
                analysis.affectedSystems().stream().map(system -> new AffectedSystemResponse(system.systemId().value(), system.label(), system.impactRing())).toList(),
                analysis.affectedIntegrations().stream().map(integration -> new AffectedIntegrationResponse(integration.integrationId(), integration.sourceSystemId().value(), integration.targetSystemId().value(), integration.protocol(), integration.dataDomain(), integration.slaClass().name(), integration.impactRing())).toList());
    }
    public record IncidentResponse(String systemId, DegradationType degradationType) { }
    public record AffectedSystemResponse(String systemId, String label, int impactRing) { }
    public record AffectedIntegrationResponse(String integrationId, String sourceSystemId, String targetSystemId, String protocol, String dataDomain, String slaClass, int impactRing) { }
}