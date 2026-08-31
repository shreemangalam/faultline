package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.ImpactAnalysis;
import dev.shreemangalam.faultline.survey.domain.ImpactAnalyzer;
import dev.shreemangalam.faultline.survey.domain.Landscape;
import org.springframework.stereotype.Service;

@Service
public class ImpactAnalysisService {

    private final Landscape landscape;
    private final ImpactAnalyzer impactAnalyzer = new ImpactAnalyzer();

    public ImpactAnalysisService(Landscape landscape) {
        this.landscape = landscape;
    }

    public ImpactAnalysis analyze(SystemId failedSystemId) {
        return impactAnalyzer.analyze(landscape, failedSystemId);
    }
}
