package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.RecoveryPlan;
import dev.shreemangalam.faultline.survey.domain.RecoverySequencePlanner;
import org.springframework.stereotype.Service;

@Service
public class RecoverySequenceService {

    private final ImpactAnalysisService impactAnalysisService;
    private final ActiveLandscapeService activeLandscapeService;
    private final RecoverySequencePlanner recoverySequencePlanner = new RecoverySequencePlanner();

    public RecoverySequenceService(ImpactAnalysisService impactAnalysisService, ActiveLandscapeService activeLandscapeService) {
        this.impactAnalysisService = impactAnalysisService;
        this.activeLandscapeService = activeLandscapeService;
    }

    public RecoveryPlan plan(SystemId failedSystemId) {
        return recoverySequencePlanner.plan(activeLandscapeService.currentLandscape(), impactAnalysisService.analyze(failedSystemId));
    }
}