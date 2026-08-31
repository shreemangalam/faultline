package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactAnalyzerTest {

    private final ImpactAnalyzer analyzer = new ImpactAnalyzer();

    @Test
    void groupsReachableSystemsByTheirShortestImpactRing() {
        Landscape landscape = landscape(
                new IntegrationEdge("A_TO_B", id("SYSTEM_A"), id("SYSTEM_B"), "REST"),
                new IntegrationEdge("A_TO_C", id("SYSTEM_A"), id("SYSTEM_C"), "REST"),
                new IntegrationEdge("B_TO_D", id("SYSTEM_B"), id("SYSTEM_D"), "REST"),
                new IntegrationEdge("C_TO_D", id("SYSTEM_C"), id("SYSTEM_D"), "REST"));

        ImpactAnalysis analysis = analyzer.analyze(landscape, id("SYSTEM_A"));

        assertThat(analysis.affectedSystems()).extracting(
                        system -> system.systemId().value() + ":" + system.impactRing())
                .containsExactly("SYSTEM_B:1", "SYSTEM_C:1", "SYSTEM_D:2");
        assertThat(analysis.affectedIntegrations()).extracting(
                        integration -> integration.integrationId() + ":" + integration.impactRing())
                .containsExactly("A_TO_B:1", "A_TO_C:1", "B_TO_D:2", "C_TO_D:2");
    }

    @Test
    void terminatesWhenTheReachableLandscapeContainsACycle() {
        Landscape landscape = landscape(
                new IntegrationEdge("A_TO_B", id("SYSTEM_A"), id("SYSTEM_B"), "REST"),
                new IntegrationEdge("B_TO_C", id("SYSTEM_B"), id("SYSTEM_C"), "REST"),
                new IntegrationEdge("C_TO_A", id("SYSTEM_C"), id("SYSTEM_A"), "REST"));

        ImpactAnalysis analysis = analyzer.analyze(landscape, id("SYSTEM_A"));

        assertThat(analysis.affectedSystems()).extracting(system -> system.systemId().value())
                .containsExactly("SYSTEM_B", "SYSTEM_C");
        assertThat(analysis.affectedIntegrations()).hasSize(3);
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
