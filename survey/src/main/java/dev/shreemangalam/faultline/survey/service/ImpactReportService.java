package dev.shreemangalam.faultline.survey.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.api.DegradationType;
import dev.shreemangalam.faultline.survey.domain.ConsumerImpactClassifier;
import dev.shreemangalam.faultline.survey.domain.ImpactAnalyzer;
import dev.shreemangalam.faultline.survey.domain.ImpactPriorityScorer;
import dev.shreemangalam.faultline.survey.domain.RecoverySequencePlanner;
import dev.shreemangalam.faultline.survey.persistence.ImpactReportEntity;
import dev.shreemangalam.faultline.survey.persistence.ImpactReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImpactReportService {
    private static final String BUNDLED_SAMPLE_REFERENCE = "BUNDLED_SAMPLE";
    private final ActiveLandscapeService activeLandscapeService;
    private final ImpactReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final ImpactAnalyzer impactAnalyzer = new ImpactAnalyzer();
    private final ImpactPriorityScorer priorityScorer = new ImpactPriorityScorer();
    private final RecoverySequencePlanner recoveryPlanner = new RecoverySequencePlanner();
    private final ConsumerImpactClassifier classifier = new ConsumerImpactClassifier();

    public ImpactReportService(ActiveLandscapeService activeLandscapeService, ImpactReportRepository reportRepository, ObjectMapper objectMapper) {
        this.activeLandscapeService = activeLandscapeService;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StoredImpactReport create(SystemId failedSystemId, DegradationType degradationType) {
        var landscape = activeLandscapeService.currentLandscape();
        var analysis = impactAnalyzer.analyze(landscape, failedSystemId);
        var priorities = priorityScorer.score(landscape, analysis);
        var recovery = recoveryPlanner.plan(landscape, analysis);
        var classifications = classifier.classify(landscape, analysis);
        StoredImpactReport.Snapshot snapshot = new StoredImpactReport.Snapshot(
                new StoredImpactReport.Incident(failedSystemId.value(), degradationType.name()),
                analysis.affectedSystems().stream().map(system -> new StoredImpactReport.AffectedSystem(
                        system.systemId().value(), system.label(), system.impactRing(), classifications.get(system.systemId()).name())).toList(),
                analysis.affectedIntegrations().stream().map(integration -> new StoredImpactReport.AffectedIntegration(
                        integration.integrationId(), integration.sourceSystemId().value(), integration.targetSystemId().value(),
                        integration.protocol(), integration.dataDomain(), integration.slaClass().name(), integration.impactRing())).toList(),
                priorities.priorities().stream().map(priority -> new StoredImpactReport.Priority(
                        priority.systemId().value(), priority.label(), priority.impactRing(), priority.score(), priority.severity().name(),
                        new StoredImpactReport.ScoreBreakdown(priority.scoreBreakdown().criticalityPoints(), priority.scoreBreakdown().fanOutPoints(), priority.scoreBreakdown().slaPoints(), priority.scoreBreakdown().proximityPoints()))).toList(),
                new StoredImpactReport.Recovery(recovery.recoverySequence().stream().map(SystemId::value).toList(), recovery.hasCycleWarning(), recovery.unresolvedSystemIds().stream().map(SystemId::value).toList()),
                recovery.hasCycleWarning() ? List.of("Dependency cycle prevents automatic recovery for: " + String.join(", ", recovery.unresolvedSystemIds().stream().map(SystemId::value).toList())) : List.of());
        UUID reportId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        String landscapeReference = activeLandscapeService.activeLandscape().map(stored -> stored.importId().toString()).orElse(BUNDLED_SAMPLE_REFERENCE);
        reportRepository.save(new ImpactReportEntity(reportId, createdAt, failedSystemId.value(), degradationType.name(), landscapeReference, write(snapshot)));
        return new StoredImpactReport(reportId, createdAt, landscapeReference, snapshot);
    }

    @Transactional(readOnly = true)
    public Optional<StoredImpactReport> findById(UUID reportId) {
        return reportRepository.findById(reportId).map(entity -> new StoredImpactReport(entity.getId(), entity.getCreatedAt(), entity.getLandscapeReference(), read(entity.getReportJson())));
    }

    private String write(StoredImpactReport.Snapshot snapshot) {
        try { return objectMapper.writeValueAsString(snapshot); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("could not persist impact report snapshot", exception); }
    }
    private StoredImpactReport.Snapshot read(String reportJson) {
        try { return objectMapper.readValue(reportJson, StoredImpactReport.Snapshot.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("stored impact report is unreadable", exception); }
    }
}