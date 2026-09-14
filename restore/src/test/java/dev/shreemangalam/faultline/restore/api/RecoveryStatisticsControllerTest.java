package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;
import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecoveryStatisticsControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clear() {
        jdbcTemplate.update("DELETE FROM restore_event_attempt");
        jdbcTemplate.update("DELETE FROM restore_event_audit");
        jdbcTemplate.update("DELETE FROM restore_event_dependency");
        jdbcTemplate.update("DELETE FROM restore_event");
        jdbcTemplate.update("DELETE FROM restore_circuit_breaker");
    }

    @Test
    void reportsPersistedOutcomesAndSuccessfulRecoveryLatency() throws Exception {
        configure("STATISTICS_SUCCESS", "SUCCESS");
        create("STATISTICS_SUCCESS");
        runOnce();

        configure("STATISTICS_RETRY", "TEMPORARY_UNAVAILABLE");
        create("STATISTICS_RETRY");
        runOnce();

        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/statistics", String.class);
        JsonNode statistics = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statistics.path("generatedAt").asText()).isNotBlank();
        assertThat(statistics.path("totalEvents").asLong()).isEqualTo(2);
        assertThat(statistics.path("pendingEventCount").asLong()).isEqualTo(1);
        assertThat(statistics.path("terminalFailureCount").asLong()).isZero();
        assertThat(statistics.path("eventCounts").size()).isEqualTo(RecoveryEventState.values().length);
        assertThat(statistics.path("eventCounts").path("COMPLETED").asLong()).isEqualTo(1);
        assertThat(statistics.path("eventCounts").path("RETRY_SCHEDULED").asLong()).isEqualTo(1);
        assertThat(statistics.path("eventCounts").path("EXHAUSTED").asLong()).isZero();
        assertThat(statistics.path("totalAttempts").asLong()).isEqualTo(2);
        assertThat(statistics.path("attemptCounts").size()).isEqualTo(RecoveryAttemptOutcome.values().length);
        assertThat(statistics.path("attemptCounts").path("SUCCEEDED").asLong()).isEqualTo(1);
        assertThat(statistics.path("attemptCounts").path("TRANSIENT_FAILURE").asLong()).isEqualTo(1);
        assertThat(statistics.path("p95SuccessfulRecoveryLatencyMillis").asLong()).isNotNegative();
    }

    private void configure(String targetSystem, String behavior) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/simulator/targets/" + targetSystem,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("behavior", behavior, "delayMillis", 0, "resetHistory", true)),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void create(String targetSystem) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/events",
                Map.of(
                        "idempotencyKey", "statistics-" + UUID.randomUUID(),
                        "correlationId", "statistics-order-" + UUID.randomUUID(),
                        "targetSystem", targetSystem,
                        "payloadReference", "local://statistics/payload.json",
                        "priority", 1),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void runOnce() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/worker/run-once",
                Map.of("workerId", "statistics-worker", "maxEvents", 1, "leaseSeconds", 30),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
