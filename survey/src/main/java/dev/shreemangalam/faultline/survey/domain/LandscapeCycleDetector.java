package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class LandscapeCycleDetector {
    public Set<SystemId> systemsInOrDownstreamOfCycles(Landscape landscape) {
        Map<SystemId, Integer> incoming = new HashMap<>();
        Map<SystemId, Set<SystemId>> outgoing = new HashMap<>();
        landscape.systems().forEach(system -> { incoming.put(system.id(), 0); outgoing.put(system.id(), new HashSet<>()); });
        for (IntegrationEdge edge : landscape.integrations()) {
            outgoing.get(edge.sourceSystemId()).add(edge.targetSystemId());
            incoming.compute(edge.targetSystemId(), (ignored, count) -> count + 1);
        }
        Queue<SystemId> ready = new ArrayDeque<>();
        incoming.forEach((systemId, count) -> { if (count == 0) { ready.add(systemId); } });
        while (!ready.isEmpty()) {
            SystemId systemId = ready.remove();
            for (SystemId target : outgoing.get(systemId)) {
                int remaining = incoming.compute(target, (ignored, count) -> count - 1);
                if (remaining == 0) { ready.add(target); }
            }
        }
        return incoming.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(() -> new java.util.TreeSet<>(Comparator.comparing(SystemId::value))));
    }
}