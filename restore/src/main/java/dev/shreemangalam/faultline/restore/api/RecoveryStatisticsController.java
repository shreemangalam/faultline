package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.service.RecoveryStatisticsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecoveryStatisticsController {
    private final RecoveryStatisticsService service;

    public RecoveryStatisticsController(RecoveryStatisticsService service) {
        this.service = service;
    }

    @GetMapping
    public RecoveryStatisticsResponse get() {
        var statistics = service.get();
        return new RecoveryStatisticsResponse(
                statistics.generatedAt(),
                statistics.totalEvents(),
                statistics.pendingEventCount(),
                statistics.terminalFailureCount(),
                statistics.eventCounts(),
                statistics.totalAttempts(),
                statistics.attemptCounts(),
                statistics.p95SuccessfulRecoveryLatencyMillis());
    }
}
