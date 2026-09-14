package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:faultline-restore-scheduled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "restore.worker.enabled=true",
                "restore.worker.id=scheduled-integration-worker",
                "restore.worker.batch-size=1",
                "restore.worker.lease-seconds=30",
                "restore.worker.fixed-delay=25ms",
                "restore.worker.initial-delay=100ms"
        })
class ScheduledRecoveryExecutionTest {
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
    void completesAnEligibleEventWithoutAManualWorkerRequest() throws Exception {
        configureSuccess("SCHEDULED_SUCCESS");
        String eventId = create("SCHEDULED_SUCCESS");

        JsonNode event = awaitState(eventId, "COMPLETED");

        assertThat(event.path("attemptCount").asInt()).isEqualTo(1);
        assertThat(event.path("attempts").path(0).path("workerId").asText())
                .isEqualTo("scheduled-integration-worker");
        assertThat(event.path("attempts").path(0).path("outcome").asText()).isEqualTo("SUCCEEDED");
    }

    private void configureSuccess(String targetSystem) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/simulator/targets/" + targetSystem,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("behavior", "SUCCESS", "delayMillis", 0, "resetHistory", true)),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String create(String targetSystem) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/events",
                Map.of(
                        "idempotencyKey", "scheduled-" + UUID.randomUUID(),
                        "correlationId", "scheduled-order-" + UUID.randomUUID(),
                        "targetSystem", targetSystem,
                        "payloadReference", "local://scheduled/payload.json",
                        "priority", 1),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).path("eventId").asText();
    }

    private JsonNode awaitState(String eventId, String expectedState) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        JsonNode event = null;
        while (System.nanoTime() < deadline) {
            event = objectMapper.readTree(restTemplate.getForObject("/api/v1/events/" + eventId, String.class));
            if (expectedState.equals(event.path("state").asText())) return event;
            Thread.sleep(25);
        }
        throw new AssertionError("Event did not reach " + expectedState + "; last response: " + event);
    }
}
