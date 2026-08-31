package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.LandscapeImportValidator;

import java.util.List;

public record LandscapeImportValidationRequest(
        List<SystemRequest> systems,
        List<IntegrationRequest> integrations) {

    LandscapeImportValidator.ImportDefinition toDefinition() {
        return new LandscapeImportValidator.ImportDefinition(
                systems == null ? null : systems.stream()
                        .map(system -> system == null ? null : new LandscapeImportValidator.ImportSystem(
                                system.id(), system.label(), system.criticality()))
                        .toList(),
                integrations == null ? null : integrations.stream()
                        .map(integration -> integration == null ? null : new LandscapeImportValidator.ImportIntegration(
                                integration.id(),
                                integration.sourceSystemId(),
                                integration.targetSystemId(),
                                integration.protocol()))
                        .toList());
    }

    public record SystemRequest(String id, String label, int criticality) {
    }

    public record IntegrationRequest(String id, String sourceSystemId, String targetSystemId, String protocol) {
    }
}
