package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerImpactClassifierTest {
    @Test
    void distinguishesOrphanedPartialAndDegradedConsumersFromExplicitDataDomains() {
        Landscape landscape = new Landscape(
                List.of(node("SOURCE_A"), node("SOURCE_B"), node("ORPHAN"), node("PARTIAL"), node("DEGRADED")),
                List.of(
                        edge("A_TO_ORPHAN", "SOURCE_A", "ORPHAN", "ORDER"),
                        edge("A_TO_PARTIAL", "SOURCE_A", "PARTIAL", "ORDER"),
                        edge("B_TO_PARTIAL", "SOURCE_B", "PARTIAL", "ORDER"),
                        edge("A_TO_DEGRADED", "SOURCE_A", "DEGRADED", "ORDER"),
                        edge("B_TO_DEGRADED", "SOURCE_B", "DEGRADED", "CUSTOMER")));
        ImpactAnalysis analysis = new ImpactAnalyzer().analyze(landscape, id("SOURCE_A"));

        var classifications = new ConsumerImpactClassifier().classify(landscape, analysis);

        assertThat(classifications).containsEntry(id("ORPHAN"), ConsumerImpactClassification.ORPHANED);
        assertThat(classifications).containsEntry(id("PARTIAL"), ConsumerImpactClassification.PARTIAL);
        assertThat(classifications).containsEntry(id("DEGRADED"), ConsumerImpactClassification.DEGRADED);
    }
    private SystemNode node(String value) { return new SystemNode(id(value), value, 5); }
    private IntegrationEdge edge(String id, String source, String target, String domain) { return new IntegrationEdge(id, id(source), id(target), "REST", domain, SlaClass.REAL_TIME); }
    private SystemId id(String value) { return new SystemId(value); }
}