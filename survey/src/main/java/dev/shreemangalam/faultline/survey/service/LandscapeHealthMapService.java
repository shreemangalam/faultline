package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.HealthStatus;
import dev.shreemangalam.faultline.survey.domain.LandscapeHealthMap;
import org.springframework.stereotype.Service;

@Service
public class LandscapeHealthMapService {

    private final ActiveLandscapeService activeLandscapeService;

    public LandscapeHealthMapService(ActiveLandscapeService activeLandscapeService) {
        this.activeLandscapeService = activeLandscapeService;
    }

    public LandscapeHealthMap healthMap() {
        var landscape = activeLandscapeService.currentLandscape();
        return new LandscapeHealthMap(
                landscape.systems().stream()
                        .map(system -> new LandscapeHealthMap.SystemHealth(
                                system.id(), system.label(), system.criticality(), HealthStatus.HEALTHY))
                        .toList(),
                landscape.integrations().stream()
                        .map(integration -> new LandscapeHealthMap.Integration(
                                integration.id(), integration.sourceSystemId(), integration.targetSystemId(), integration.protocol(), integration.dataDomain(), integration.slaClass()))
                        .toList());
    }
}