package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecoverySequencePlannerTest {

    private final ImpactAnalyzer impactAnalyzer = new ImpactAnalyzer();
    private final RecoverySequencePlanner planner = new RecoverySequencePlanner();

    @Test
    void ordersAnAcyclicAffectedSubgraphWithStableTies() {
        Landscape landscape = landscape(
                new IntegrationEdge("A_TO_C", id("SYSTEM_A"), id("SYSTEM_C"), "REST"),
                new IntegrationEdge("A_TO_B", id("SYSTEM_A"), id("SYSTEM_B"), "REST"),
                new IntegrationEdge("B_TO_D", id("SYSTEM_B"), id("SYSTEM_D"), "REST"),
                new IntegrationEdge("C_TO_D", id("SYSTEM_C"), id("SYSTEM_D"), "REST"));

        RecoveryPlan plan = planner.plan(landscape, impactAnalyzer.analyze(landscape, id("SYSTEM_A")));

        assertThat(plan.recoverySequence()).containsExactly(id("SYSTEM_A"), id("SYSTEM_B"), id("SYSTEM_C"), id("SYSTEM_D"));
        assertThat(plan.hasCycleWarning()).isFalse();
        assertThat(plan.unresolvedSystemIds()).isEmpty();
    }

    @Test
    void returnsOnlyTheSafePrefixWhenACyclePreventsFurtherOrdering() {
        Landscape landscape = landscape(
                new IntegrationEdge("A_TO_B", id("SYSTEM_A"), id("SYSTEM_B"), "REST"),
                new IntegrationEdge("A_TO_D", id("SYSTEM_A"), id("SYSTEM_D"), "REST"),
                new IntegrationEdge("B_TO_C", id("SYSTEM_B"), id("SYSTEM_C"), "REST"),
                new IntegrationEdge("C_TO_B", id("SYSTEM_C"), id("SYSTEM_B"), "REST"));

        RecoveryPlan plan = planner.plan(landscape, impactAnalyzer.analyze(landscape, id("SYSTEM_A")));

        assertThat(plan.recoverySequence()).containsExactly(id("SYSTEM_A"), id("SYSTEM_D"));
        assertThat(plan.hasCycleWarning()).isTrue();
        assertThat(plan.unresolvedSystemIds()).containsExactly(id("SYSTEM_B"), id("SYSTEM_C"));
    }

    private Landscape landscape(IntegrationEdge... integrations) {
        return new Landscape(
                List.of(
                        new SystemNode(id("SYSTEM_A"), "System A", 5),
                        new SystemNode(id("SYSTEM_B"), "System B", 5),
                        new SystemNode(id("SYSTEM_C"), "System C", 5),
                        new SystemNode(id("SYSTEM_D"), "System D", 5)),
                List.of(integrations));
    }

    private SystemId id(String value) {
        return new SystemId(value);
    }
}
