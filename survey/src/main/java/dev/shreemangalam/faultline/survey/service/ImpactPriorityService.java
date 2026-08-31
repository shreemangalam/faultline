package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.ImpactPriorityReport;
import dev.shreemangalam.faultline.survey.domain.ImpactPriorityScorer;
import org.springframework.stereotype.Service;

@Service
public class ImpactPriorityService {

    private final ImpactAnalysisService impactAnalysisService;
    private final ActiveLandscapeService activeLandscapeService;
    private final ImpactPriorityScorer impactPriorityScorer = new ImpactPriorityScorer();

    public ImpactPriorityService(ImpactAnalysisService impactAnalysisService, ActiveLandscapeService activeLandscapeService) {
        this.impactAnalysisService = impactAnalysisService;
        this.activeLandscapeService = activeLandscapeService;
    }

    public ImpactPriorityReport prioritize(SystemId failedSystemId) {
        return impactPriorityScorer.score(activeLandscapeService.currentLandscape(), impactAnalysisService.analyze(failedSystemId));
    }
}