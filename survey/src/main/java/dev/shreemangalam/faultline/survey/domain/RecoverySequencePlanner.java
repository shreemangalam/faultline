package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class RecoverySequencePlanner {

    public RecoveryPlan plan(Landscape landscape, ImpactAnalysis analysis) {
        Set<SystemId> affectedSystemIds = new HashSet<>();
        affectedSystemIds.add(analysis.failedSystemId());
        analysis.affectedSystems().forEach(system -> affectedSystemIds.add(system.systemId()));

        Map<SystemId, Integer> incomingDependencies = new HashMap<>();
        Map<SystemId, List<SystemId>> dependants = new HashMap<>();
        affectedSystemIds.forEach(systemId -> {
            incomingDependencies.put(systemId, 0);
            dependants.put(systemId, new ArrayList<>());
        });
        for (IntegrationEdge integration : landscape.integrations()) {
            if (affectedSystemIds.contains(integration.sourceSystemId())
                    && affectedSystemIds.contains(integration.targetSystemId())) {
                dependants.get(integration.sourceSystemId()).add(integration.targetSystemId());
                incomingDependencies.compute(integration.targetSystemId(), (systemId, count) -> count + 1);
            }
        }

        PriorityQueue<SystemId> readySystems = new PriorityQueue<>(Comparator.comparing(SystemId::value));
        incomingDependencies.forEach((systemId, count) -> {
            if (count == 0) {
                readySystems.add(systemId);
            }
        });

        List<SystemId> recoverySequence = new ArrayList<>();
        while (!readySystems.isEmpty()) {
            SystemId systemId = readySystems.remove();
            recoverySequence.add(systemId);
            for (SystemId dependant : dependants.get(systemId)) {
                int remainingDependencies = incomingDependencies.compute(dependant, (ignored, count) -> count - 1);
                if (remainingDependencies == 0) {
                    readySystems.add(dependant);
                }
            }
        }

        List<SystemId> unresolvedSystemIds = incomingDependencies.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing(SystemId::value))
                .toList();
        return new RecoveryPlan(analysis.failedSystemId(), recoverySequence, unresolvedSystemIds);
    }
}
