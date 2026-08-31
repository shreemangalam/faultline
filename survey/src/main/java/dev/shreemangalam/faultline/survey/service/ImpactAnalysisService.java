package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.ImpactAnalysis;
import dev.shreemangalam.faultline.survey.domain.ImpactAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class ImpactAnalysisService {

    private final ActiveLandscapeService activeLandscapeService;
    private final ImpactAnalyzer impactAnalyzer = new ImpactAnalyzer();

    public ImpactAnalysisService(ActiveLandscapeService activeLandscapeService) {
        this.activeLandscapeService = activeLandscapeService;
    }

    public ImpactAnalysis analyze(SystemId failedSystemId) {
        return impactAnalyzer.analyze(activeLandscapeService.currentLandscape(), failedSystemId);
    }
}