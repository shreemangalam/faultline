package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ImpactPriorityScorer {

    public ImpactPriorityReport score(Landscape landscape, ImpactAnalysis analysis) {
        Map<SystemId, SystemNode> systemsById = new HashMap<>();
        for (SystemNode system : landscape.systems()) {
            systemsById.put(system.id(), system);
        }

        List<ImpactPriorityReport.ImpactPriority> priorities = analysis.affectedSystems().stream()
                .map(affectedSystem -> score(landscape, analysis, systemsById, affectedSystem))
                .sorted(Comparator.comparingInt(ImpactPriorityReport.ImpactPriority::score).reversed()
                        .thenComparing(priority -> priority.systemId().value()))
                .toList();

        return new ImpactPriorityReport(analysis.failedSystemId(), priorities);
    }

    private ImpactPriorityReport.ImpactPriority score(
            Landscape landscape,
            ImpactAnalysis analysis,
            Map<SystemId, SystemNode> systemsById,
            ImpactAnalysis.AffectedSystem affectedSystem) {
        SystemNode system = systemsById.get(affectedSystem.systemId());
        int criticalityPoints = system.criticality() * 10;
        int fanOutPoints = (int) Math.min(20, landscape.integrations().stream()
                .filter(integration -> integration.sourceSystemId().equals(system.id()))
                .count() * 2);
        int slaPoints = incomingIntegration(analysis, affectedSystem).map(integration -> integration.slaClass().impactPoints())
                .orElse(0);
        int proximityPoints = Math.max(0, 20 - ((affectedSystem.impactRing() - 1) * 5));
        ImpactPriorityReport.ScoreBreakdown breakdown = new ImpactPriorityReport.ScoreBreakdown(
                criticalityPoints, fanOutPoints, slaPoints, proximityPoints);

        return new ImpactPriorityReport.ImpactPriority(
                system.id(), system.label(), affectedSystem.impactRing(), breakdown.total(),
                SeverityBand.fromScore(breakdown.total()), breakdown);
    }

    private java.util.Optional<ImpactAnalysis.AffectedIntegration> incomingIntegration(
            ImpactAnalysis analysis,
            ImpactAnalysis.AffectedSystem affectedSystem) {
        return analysis.affectedIntegrations().stream()
                .filter(integration -> integration.targetSystemId().equals(affectedSystem.systemId()))
                .filter(integration -> integration.impactRing() == affectedSystem.impactRing())
                .findFirst();
    }
}
