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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "restore.downstream.read-timeout=100ms")
class RecoveryBatchExecutionTest {
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
    void completesAnEventAfterARealSuccessfulHttpDelivery() throws Exception {
        configure("EXECUTION_SUCCESS", "SUCCESS", 0);
        String eventId = create("EXECUTION_SUCCESS");

        JsonNode result = runOnce();
        assertThat(result.path(0).path("eventId").asText()).isEqualTo(eventId);
        assertThat(result.path(0).path("state").asText()).isEqualTo("COMPLETED");
        assertThat(result.path(0).path("classification").asText()).isEqualTo("SUCCESS");
        assertThat(result.path(0).path("httpStatus").asInt()).isEqualTo(200);

        JsonNode event = getEvent(eventId);
        assertThat(event.path("attempts").path(0).path("outcome").asText()).isEqualTo("SUCCEEDED");
        assertThat(event.path("attempts").path(0).path("httpStatus").asInt()).isEqualTo(200);
        assertThat(event.path("attempts").path(0).path("durationMillis").asLong()).isNotNegative();
        assertThat(getSimulator("EXECUTION_SUCCESS").path("acceptedCount").asLong()).isEqualTo(1);
    }

    @Test
    void schedulesRetryForRetriableHttpResponses() throws Exception {
        configure("EXECUTION_TRANSIENT", "TEMPORARY_UNAVAILABLE", 0);
        String eventId = create("EXECUTION_TRANSIENT");

        JsonNode result = runOnce();
        assertThat(result.path(0).path("state").asText()).isEqualTo("RETRY_SCHEDULED");
        assertThat(result.path(0).path("classification").asText()).isEqualTo("TRANSIENT_FAILURE");
        assertThat(result.path(0).path("httpStatus").asInt()).isEqualTo(503);

        JsonNode event = getEvent(eventId);
        assertThat(event.path("attempts").path(0).path("outcome").asText()).isEqualTo("TRANSIENT_FAILURE");
        assertThat(event.path("attempts").path(0).path("httpStatus").asInt()).isEqualTo(503);
        assertThat(circuit("EXECUTION_TRANSIENT").path("consecutiveFailures").asInt()).isEqualTo(1);
    }

    @Test
    void terminatesNonRetriableHttpResponsesWithoutOpeningTheCircuit() throws Exception {
        configure("EXECUTION_PERMANENT", "VALIDATION_FAILURE", 0);
        String eventId = create("EXECUTION_PERMANENT");

        JsonNode result = runOnce();
        assertThat(result.path(0).path("state").asText()).isEqualTo("FAILED_FINAL");
        assertThat(result.path(0).path("classification").asText()).isEqualTo("PERMANENT_FAILURE");
        assertThat(result.path(0).path("httpStatus").asInt()).isEqualTo(422);

        JsonNode event = getEvent(eventId);
        assertThat(event.path("attempts").path(0).path("outcome").asText()).isEqualTo("PERMANENT_FAILURE");
        assertThat(event.path("attempts").path(0).path("httpStatus").asInt()).isEqualTo(422);
        assertThat(circuit("EXECUTION_PERMANENT").path("state").asText()).isEqualTo("CLOSED");
        assertThat(circuit("EXECUTION_PERMANENT").path("consecutiveFailures").asInt()).isZero();
    }

    @Test
    void replaysAnAmbiguousTimeoutWithoutDuplicatingTheDownstreamSideEffect() throws Exception {
        configure("EXECUTION_TIMEOUT", "DELAYED_SUCCESS", 500);
        String eventId = create("EXECUTION_TIMEOUT");

        JsonNode timedOut = runOnce();
        assertThat(timedOut.path(0).path("state").asText()).isEqualTo("RETRY_SCHEDULED");
        assertThat(timedOut.path(0).path("classification").asText()).isEqualTo("TRANSIENT_FAILURE");
        assertThat(timedOut.path(0).path("httpStatus").isNull()).isTrue();

        JsonNode firstAttempt = getEvent(eventId).path("attempts").path(0);
        assertThat(firstAttempt.path("outcome").asText()).isEqualTo("TRANSIENT_FAILURE");
        assertThat(firstAttempt.path("httpStatus").isNull()).isTrue();
        assertThat(firstAttempt.path("failureReason").asText()).contains("timed out");

        awaitAcceptedDelivery("EXECUTION_TIMEOUT");
        configure("EXECUTION_TIMEOUT", "SUCCESS", 0, false);
        jdbcTemplate.update(
                "UPDATE restore_event SET next_eligible_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)),
                UUID.fromString(eventId));

        JsonNode replayed = runOnce();
        assertThat(replayed.path(0).path("state").asText()).isEqualTo("COMPLETED");
        assertThat(replayed.path(0).path("classification").asText()).isEqualTo("SUCCESS");
        assertThat(replayed.path(0).path("httpStatus").asInt()).isEqualTo(200);

        JsonNode event = getEvent(eventId);
        assertThat(event.path("attempts").size()).isEqualTo(2);
        assertThat(event.path("attempts").path(1).path("outcome").asText()).isEqualTo("SUCCEEDED");
        assertThat(event.path("attempts").path(1).path("httpStatus").asInt()).isEqualTo(200);
        assertThat(getSimulator("EXECUTION_TIMEOUT").path("requestCount").asLong()).isEqualTo(2);
        assertThat(getSimulator("EXECUTION_TIMEOUT").path("acceptedCount").asLong()).isEqualTo(1);
    }

    private void configure(String targetSystem, String behavior, long delayMillis) {
        configure(targetSystem, behavior, delayMillis, true);
    }

    private void configure(String targetSystem, String behavior, long delayMillis, boolean resetHistory) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/simulator/targets/" + targetSystem,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "behavior", behavior,
                        "delayMillis", delayMillis,
                        "resetHistory", resetHistory)),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void awaitAcceptedDelivery(String targetSystem) throws Exception {
        for (int attempt = 0; attempt < 80; attempt++) {
            if (getSimulator(targetSystem).path("acceptedCount").asLong() == 1) return;
            Thread.sleep(25);
        }
        throw new AssertionError("Timed-out delivery was not eventually accepted by " + targetSystem);
    }

    private String create(String targetSystem) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/events",
                Map.of(
                        "idempotencyKey", "execution-" + UUID.randomUUID(),
                        "correlationId", "execution-order-" + UUID.randomUUID(),
                        "targetSystem", targetSystem,
                        "payloadReference", "local://execution/payload.json",
                        "priority", 1),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).path("eventId").asText();
    }

    private JsonNode runOnce() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/worker/run-once",
                Map.of("workerId", "http-executor", "maxEvents", 1, "leaseSeconds", 30),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode getEvent(String eventId) throws Exception {
        return objectMapper.readTree(restTemplate.getForObject("/api/v1/events/" + eventId, String.class));
    }

    private JsonNode getSimulator(String targetSystem) throws Exception {
        return objectMapper.readTree(restTemplate.getForObject("/api/v1/simulator/targets/" + targetSystem, String.class));
    }

    private JsonNode circuit(String targetSystem) throws Exception {
        JsonNode circuits = objectMapper.readTree(restTemplate.getForObject("/api/v1/circuit-breakers", String.class));
        for (JsonNode circuit : circuits) {
            if (targetSystem.equals(circuit.path("targetSystem").asText())) return circuit;
        }
        throw new AssertionError("Circuit not found for " + targetSystem);
    }
}
