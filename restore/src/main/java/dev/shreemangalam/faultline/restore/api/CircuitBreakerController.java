package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.service.CircuitBreakerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/circuit-breakers", produces = MediaType.APPLICATION_JSON_VALUE)
public class CircuitBreakerController {
    private final CircuitBreakerService service;

    public CircuitBreakerController(CircuitBreakerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CircuitBreakerResponse> list() {
        return service.list().stream()
                .map(circuit -> new CircuitBreakerResponse(circuit.targetSystem(), circuit.state(), circuit.consecutiveFailures(), circuit.openUntil(), circuit.updatedAt()))
                .toList();
    }
}