package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.service.RecoveryScenarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecoveryScenarioController {
    private final RecoveryScenarioService service;

    public RecoveryScenarioController(RecoveryScenarioService service) {
        this.service = service;
    }

    @PostMapping("/dependency-cascade")
    @ResponseStatus(HttpStatus.CREATED)
    public DependencyCascadeScenarioResponse loadDependencyCascade() {
        var scenario = service.loadDependencyCascade();
        var events = scenario.events().stream()
                .map(event -> new ScenarioRecoveryEventResponse(
                        event.step(), event.eventId(), event.targetSystem(), event.state(), event.prerequisiteEventIds()))
                .toList();
        return new DependencyCascadeScenarioResponse(
                scenario.scenarioId(),
                "DEPENDENCY_CASCADE",
                scenario.correlationId(),
                events.size(),
                events);
    }
}
