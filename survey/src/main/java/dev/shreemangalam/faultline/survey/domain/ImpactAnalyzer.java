package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class ImpactAnalyzer {

    public ImpactAnalysis analyze(Landscape landscape, SystemId failedSystemId) {
        if (!landscape.containsSystem(failedSystemId)) {
            throw new UnknownSystemException(failedSystemId);
        }

        Map<SystemId, SystemNode> systemsById = new HashMap<>();
        Map<SystemId, List<IntegrationEdge>> outgoingEdges = new HashMap<>();
        for (SystemNode system : landscape.systems()) {
            systemsById.put(system.id(), system);
            outgoingEdges.put(system.id(), new ArrayList<>());
        }
        for (IntegrationEdge edge : landscape.integrations()) {
            outgoingEdges.get(edge.sourceSystemId()).add(edge);
        }

        Map<SystemId, Integer> distances = new LinkedHashMap<>();
        Queue<SystemId> toVisit = new ArrayDeque<>();
        distances.put(failedSystemId, 0);
        toVisit.add(failedSystemId);

        List<ImpactAnalysis.AffectedIntegration> affectedIntegrations = new ArrayList<>();
        while (!toVisit.isEmpty()) {
            SystemId sourceSystemId = toVisit.remove();
            int sourceDistance = distances.get(sourceSystemId);
            for (IntegrationEdge edge : outgoingEdges.get(sourceSystemId)) {
                int impactRing = sourceDistance + 1;
                affectedIntegrations.add(new ImpactAnalysis.AffectedIntegration(
                        edge.id(), edge.sourceSystemId(), edge.targetSystemId(), edge.protocol(), impactRing));
                if (!distances.containsKey(edge.targetSystemId())) {
                    distances.put(edge.targetSystemId(), impactRing);
                    toVisit.add(edge.targetSystemId());
                }
            }
        }

        List<ImpactAnalysis.AffectedSystem> affectedSystems = distances.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new ImpactAnalysis.AffectedSystem(
                        entry.getKey(), systemsById.get(entry.getKey()).label(), entry.getValue()))
                .toList();

        return new ImpactAnalysis(failedSystemId, affectedSystems, affectedIntegrations);
    }
}
