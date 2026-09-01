package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.survey.domain.LandscapeCycleDetector;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LandscapeWarningService {
    private final ActiveLandscapeService activeLandscapeService;
    private final LandscapeCycleDetector cycleDetector = new LandscapeCycleDetector();
    public LandscapeWarningService(ActiveLandscapeService activeLandscapeService) { this.activeLandscapeService = activeLandscapeService; }
    public List<String> warnings() {
        var unresolved = cycleDetector.systemsInOrDownstreamOfCycles(activeLandscapeService.currentLandscape());
        return unresolved.isEmpty() ? List.of() : List.of("Landscape contains a dependency cycle involving: " + String.join(", ", unresolved.stream().map(systemId -> systemId.value()).toList()));
    }
}