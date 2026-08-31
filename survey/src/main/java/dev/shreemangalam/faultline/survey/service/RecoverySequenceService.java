package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.Landscape;
import dev.shreemangalam.faultline.survey.domain.RecoveryPlan;
import dev.shreemangalam.faultline.survey.domain.RecoverySequencePlanner;
import org.springframework.stereotype.Service;

@Service
public class RecoverySequenceService {

    private final ImpactAnalysisService impactAnalysisService;
    private final Landscape landscape;
    private final RecoverySequencePlanner recoverySequencePlanner = new RecoverySequencePlanner();

    public RecoverySequenceService(ImpactAnalysisService impactAnalysisService, Landscape landscape) {
        this.impactAnalysisService = impactAnalysisService;
        this.landscape = landscape;
    }

    public RecoveryPlan plan(SystemId failedSystemId) {
        return recoverySequencePlanner.plan(landscape, impactAnalysisService.analyze(failedSystemId));
    }
}
