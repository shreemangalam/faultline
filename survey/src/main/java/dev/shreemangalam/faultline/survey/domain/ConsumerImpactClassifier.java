package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConsumerImpactClassifier {

    public Map<SystemId, ConsumerImpactClassification> classify(Landscape landscape, ImpactAnalysis analysis) {
        Set<String> impactedIntegrationIds = analysis.affectedIntegrations().stream()
                .map(ImpactAnalysis.AffectedIntegration::integrationId)
                .collect(Collectors.toSet());
        Set<SystemId> unavailableSystems = new HashSet<>();
        unavailableSystems.add(analysis.failedSystemId());
        analysis.affectedSystems().forEach(system -> unavailableSystems.add(system.systemId()));

        return analysis.affectedSystems().stream().collect(Collectors.toMap(
                ImpactAnalysis.AffectedSystem::systemId,
                affectedSystem -> classify(landscape, affectedSystem.systemId(), impactedIntegrationIds, unavailableSystems),
                (left, right) -> left));
    }

    private ConsumerImpactClassification classify(
            Landscape landscape,
            SystemId targetSystemId,
            Set<String> impactedIntegrationIds,
            Set<SystemId> unavailableSystems) {
        List<IntegrationEdge> incoming = landscape.integrations().stream()
                .filter(integration -> integration.targetSystemId().equals(targetSystemId))
                .toList();
        List<IntegrationEdge> impacted = incoming.stream()
                .filter(integration -> impactedIntegrationIds.contains(integration.id()))
                .toList();
        boolean viableAlternativeForImpactedDomain = impacted.stream().anyMatch(impactedIntegration -> incoming.stream()
                .filter(candidate -> candidate.dataDomain().equals(impactedIntegration.dataDomain()))
                .anyMatch(candidate -> !impactedIntegrationIds.contains(candidate.id())
                        && !unavailableSystems.contains(candidate.sourceSystemId())));
        if (viableAlternativeForImpactedDomain) {
            return ConsumerImpactClassification.PARTIAL;
        }
        if (!incoming.isEmpty() && incoming.stream().allMatch(integration -> impactedIntegrationIds.contains(integration.id()))) {
            return ConsumerImpactClassification.ORPHANED;
        }
        return ConsumerImpactClassification.DEGRADED;
    }
}