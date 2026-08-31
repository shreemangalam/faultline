package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactPriorityScorerTest {

    private final ImpactAnalyzer analyzer = new ImpactAnalyzer();
    private final ImpactPriorityScorer scorer = new ImpactPriorityScorer();

    @Test
    void ranksScoresAndExposesEachScoreComponent() {
        Landscape landscape = new Landscape(
                List.of(
                        new SystemNode(id("SYSTEM_A"), "System A", 5),
                        new SystemNode(id("SYSTEM_B"), "System B", 10),
                        new SystemNode(id("SYSTEM_C"), "System C", 6),
                        new SystemNode(id("SYSTEM_D"), "System D", 5)),
                List.of(
                        new IntegrationEdge("A_TO_B", id("SYSTEM_A"), id("SYSTEM_B"), "REST", "ORDER", SlaClass.REAL_TIME),
                        new IntegrationEdge("A_TO_C", id("SYSTEM_A"), id("SYSTEM_C"), "IDOC", "INVOICE", SlaClass.NEAR_REAL_TIME),
                        new IntegrationEdge("B_TO_D", id("SYSTEM_B"), id("SYSTEM_D"), "REST")));

        ImpactPriorityReport report = scorer.score(landscape, analyzer.analyze(landscape, id("SYSTEM_A")));

        ImpactPriorityReport.ImpactPriority highestPriority = report.priorities().getFirst();
        assertThat(highestPriority.systemId()).isEqualTo(id("SYSTEM_B"));
        assertThat(highestPriority.score()).isEqualTo(142);
        assertThat(highestPriority.severity()).isEqualTo(SeverityBand.CRITICAL);
        assertThat(highestPriority.scoreBreakdown())
                .isEqualTo(new ImpactPriorityReport.ScoreBreakdown(100, 2, 20, 20));
    }

    private SystemId id(String value) {
        return new SystemId(value);
    }
}
