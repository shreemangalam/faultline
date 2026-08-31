package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.ImpactPriorityReport;
import dev.shreemangalam.faultline.survey.domain.ImpactPriorityScorer;
import dev.shreemangalam.faultline.survey.domain.Landscape;
import org.springframework.stereotype.Service;

@Service
public class ImpactPriorityService {

    private final ImpactAnalysisService impactAnalysisService;
    private final Landscape landscape;
    private final ImpactPriorityScorer impactPriorityScorer = new ImpactPriorityScorer();

    public ImpactPriorityService(ImpactAnalysisService impactAnalysisService, Landscape landscape) {
        this.impactAnalysisService = impactAnalysisService;
        this.landscape = landscape;
    }

    public ImpactPriorityReport prioritize(SystemId failedSystemId) {
        return impactPriorityScorer.score(landscape, impactAnalysisService.analyze(failedSystemId));
    }
}
