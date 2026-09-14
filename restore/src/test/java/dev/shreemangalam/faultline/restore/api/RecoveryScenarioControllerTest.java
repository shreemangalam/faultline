package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecoveryScenarioControllerTest {
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
    void loadsAndExecutesAnIsolatedDependencyCascade() throws Exception {
        ResponseEntity<String> loadedResponse = restTemplate.postForEntity(
                "/api/v1/scenarios/dependency-cascade", null, String.class);
        JsonNode loaded = objectMapper.readTree(loadedResponse.getBody());

        assertThat(loadedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(loaded.path("scenario").asText()).isEqualTo("DEPENDENCY_CASCADE");
        assertThat(loaded.path("scenarioId").asText()).isNotBlank();
        assertThat(loaded.path("correlationId").asText()).startsWith("dependency-cascade-");
        assertThat(loaded.path("eventCount").asInt()).isEqualTo(6);
        assertThat(loaded.path("events")).hasSize(6);
        assertThat(states(loaded.path("events"))).containsExactly(
                "ELIGIBLE", "BLOCKED", "BLOCKED", "BLOCKED", "BLOCKED", "BLOCKED");
        assertThat(steps(loaded.path("events"))).containsExactly(
                "CUSTOMER_VALIDATED",
                "ORDER_CREATED",
                "INVENTORY_RESERVED",
                "PAYMENT_CAPTURED",
                "SHIPMENT_SCHEDULED",
                "ORDER_CONFIRMED");
        assertThat(loaded.path("events").path(5).path("prerequisiteEventIds")).hasSize(2);

        List<Integer> batchSizes = new ArrayList<>();
        for (int batch = 0; batch < 5; batch++) batchSizes.add(runOnce().size());

        assertThat(batchSizes).containsExactly(1, 1, 2, 1, 1);
        JsonNode statistics = statistics();
        assertThat(statistics.path("totalEvents").asLong()).isEqualTo(6);
        assertThat(statistics.path("pendingEventCount").asLong()).isZero();
        assertThat(statistics.path("eventCounts").path("COMPLETED").asLong()).isEqualTo(6);
        assertThat(statistics.path("attemptCounts").path("SUCCEEDED").asLong()).isEqualTo(6);
    }

    @Test
    void loadsPriorityDeadlineScenarioInItsDeclaredAdmissionOrder() throws Exception {
        ResponseEntity<String> loadedResponse = restTemplate.postForEntity(
                "/api/v1/scenarios/priority-deadline", null, String.class);
        JsonNode loaded = objectMapper.readTree(loadedResponse.getBody());

        assertThat(loadedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(loaded.path("scenario").asText()).isEqualTo("PRIORITY_DEADLINE");
        assertThat(loaded.path("eventCount").asInt()).isEqualTo(7);
        assertThat(loaded.path("events")).hasSize(7);
        assertThat(steps(loaded.path("events"))).containsExactly(
                "BACKLOG_CLEANUP",
                "STANDARD_ORDER",
                "FANOUT_RELEASE",
                "INVENTORY_BRANCH",
                "SHIPPING_BRANCH",
                "SLA_RISK",
                "CUSTOMER_ESCALATION");
        assertThat(states(loaded.path("events"))).containsExactly(
                "ELIGIBLE", "ELIGIBLE", "ELIGIBLE", "BLOCKED", "BLOCKED", "ELIGIBLE", "ELIGIBLE");
        assertThat(textValues(loaded.path("expectedAdmissionOrder"))).containsExactly(
                "CUSTOMER_ESCALATION", "SLA_RISK", "FANOUT_RELEASE", "STANDARD_ORDER", "BACKLOG_CLEANUP");

        Map<String, String> eventIdsByStep = new HashMap<>();
        loaded.path("events").forEach(event -> eventIdsByStep.put(
                event.path("step").asText(), event.path("eventId").asText()));
        JsonNode claimed = objectMapper.readTree(restTemplate.postForObject(
                "/api/v1/worker/claims",
                Map.of("workerId", "priority-scenario-worker", "maxEvents", 10, "leaseSeconds", 30),
                String.class));
        List<String> claimedIds = new ArrayList<>();
        claimed.forEach(event -> claimedIds.add(event.path("eventId").asText()));

        assertThat(claimedIds).containsExactly(
                eventIdsByStep.get("CUSTOMER_ESCALATION"),
                eventIdsByStep.get("SLA_RISK"),
                eventIdsByStep.get("FANOUT_RELEASE"),
                eventIdsByStep.get("STANDARD_ORDER"),
                eventIdsByStep.get("BACKLOG_CLEANUP"));
    }

    @Test
    void drivesPoisonAndRetryExhaustionPathsIntoQueryableTerminalStates() throws Exception {
        ResponseEntity<String> loadedResponse = restTemplate.postForEntity(
                "/api/v1/scenarios/terminal-failures", null, String.class);
        JsonNode loaded = objectMapper.readTree(loadedResponse.getBody());

        assertThat(loadedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(loaded.path("scenario").asText()).isEqualTo("TERMINAL_FAILURES");
        assertThat(loaded.path("eventCount").asInt()).isEqualTo(5);
        assertThat(steps(loaded.path("events"))).containsExactly(
                "INVALID_CUSTOMER_UPDATE",
                "CUSTOMER_REVIEW",
                "STUCK_ORDER_REPLAY",
                "ORDER_RECONCILIATION",
                "CUSTOMER_NOTIFICATION");
        assertThat(states(loaded.path("events"))).containsExactly(
                "ELIGIBLE", "BLOCKED", "ELIGIBLE", "BLOCKED", "BLOCKED");
        assertThat(textFieldValues(loaded.path("failurePaths"), "path")).containsExactly(
                "POISON_MESSAGE", "RETRY_EXHAUSTION");
        assertThat(textFieldValues(loaded.path("failurePaths"), "downstreamBehavior")).containsExactly(
                "VALIDATION_FAILURE", "TEMPORARY_UNAVAILABLE");
        assertThat(textFieldValues(loaded.path("failurePaths"), "expectedTerminalState")).containsExactly(
                "FAILED_FINAL", "EXHAUSTED");
        assertThat(intFieldValues(loaded.path("failurePaths"), "expectedAttempts")).containsExactly(1, 3);

        Map<String, JsonNode> eventsByStep = nodesByField(loaded.path("events"), "step");
        String invalidUpdateId = eventsByStep.get("INVALID_CUSTOMER_UPDATE").path("eventId").asText();
        String customerReviewId = eventsByStep.get("CUSTOMER_REVIEW").path("eventId").asText();
        String stuckReplayId = eventsByStep.get("STUCK_ORDER_REPLAY").path("eventId").asText();
        String reconciliationId = eventsByStep.get("ORDER_RECONCILIATION").path("eventId").asText();
        String notificationId = eventsByStep.get("CUSTOMER_NOTIFICATION").path("eventId").asText();
        String permanentTarget = loaded.path("failurePaths").path(0).path("targetSystem").asText();
        String retryTarget = loaded.path("failurePaths").path(1).path("targetSystem").asText();

        JsonNode firstBatch = runOnce();
        assertThat(firstBatch).hasSize(2);
        assertThat(firstBatch.path(0).path("eventId").asText()).isEqualTo(invalidUpdateId);
        assertThat(firstBatch.path(0).path("state").asText()).isEqualTo("FAILED_FINAL");
        assertThat(firstBatch.path(0).path("classification").asText()).isEqualTo("PERMANENT_FAILURE");
        assertThat(firstBatch.path(1).path("eventId").asText()).isEqualTo(stuckReplayId);
        assertThat(firstBatch.path(1).path("state").asText()).isEqualTo("RETRY_SCHEDULED");
        assertThat(firstBatch.path(1).path("classification").asText()).isEqualTo("TRANSIENT_FAILURE");

        forceRetryDue(stuckReplayId);
        JsonNode secondBatch = runOnce();
        assertThat(secondBatch).hasSize(1);
        assertThat(secondBatch.path(0).path("eventId").asText()).isEqualTo(stuckReplayId);
        assertThat(secondBatch.path(0).path("state").asText()).isEqualTo("RETRY_SCHEDULED");

        forceRetryDue(stuckReplayId);
        JsonNode finalBatch = runOnce();
        assertThat(finalBatch).hasSize(1);
        assertThat(finalBatch.path(0).path("eventId").asText()).isEqualTo(stuckReplayId);
        assertThat(finalBatch.path(0).path("state").asText()).isEqualTo("EXHAUSTED");

        assertThat(event(invalidUpdateId).path("state").asText()).isEqualTo("FAILED_FINAL");
        assertThat(event(invalidUpdateId).path("attemptCount").asInt()).isEqualTo(1);
        assertThat(textFieldValues(event(invalidUpdateId).path("attempts"), "outcome"))
                .containsExactly("PERMANENT_FAILURE");
        assertThat(event(customerReviewId).path("state").asText()).isEqualTo("FAILED_DEPENDENCY");
        assertThat(event(stuckReplayId).path("state").asText()).isEqualTo("EXHAUSTED");
        assertThat(event(stuckReplayId).path("attemptCount").asInt()).isEqualTo(3);
        assertThat(textFieldValues(event(stuckReplayId).path("attempts"), "outcome"))
                .containsExactly("TRANSIENT_FAILURE", "TRANSIENT_FAILURE", "EXHAUSTED");
        assertThat(event(reconciliationId).path("state").asText()).isEqualTo("FAILED_DEPENDENCY");
        assertThat(event(notificationId).path("state").asText()).isEqualTo("FAILED_DEPENDENCY");

        assertThat(eventIdsByState("FAILED_FINAL")).containsExactly(invalidUpdateId);
        assertThat(eventIdsByState("EXHAUSTED")).containsExactly(stuckReplayId);
        JsonNode statistics = statistics();
        assertThat(statistics.path("pendingEventCount").asLong()).isZero();
        assertThat(statistics.path("terminalFailureCount").asLong()).isEqualTo(5);
        assertThat(statistics.path("attemptCounts").path("PERMANENT_FAILURE").asLong()).isEqualTo(1);
        assertThat(statistics.path("attemptCounts").path("TRANSIENT_FAILURE").asLong()).isEqualTo(2);
        assertThat(statistics.path("attemptCounts").path("EXHAUSTED").asLong()).isEqualTo(1);
        assertThat(circuit(permanentTarget).path("state").asText()).isEqualTo("CLOSED");
        assertThat(circuit(permanentTarget).path("consecutiveFailures").asInt()).isZero();
        assertThat(circuit(retryTarget).path("state").asText()).isEqualTo("OPEN");
        assertThat(circuit(retryTarget).path("consecutiveFailures").asInt()).isEqualTo(3);
        assertThat(simulator(permanentTarget).path("requestCount").asLong()).isEqualTo(1);
        assertThat(simulator(retryTarget).path("requestCount").asLong()).isEqualTo(3);
    }

    private JsonNode runOnce() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/worker/run-once",
                Map.of("workerId", "scenario-worker", "maxEvents", 10, "leaseSeconds", 30),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode statistics() throws Exception {
        return objectMapper.readTree(restTemplate.getForObject("/api/v1/statistics", String.class));
    }

    private JsonNode event(String eventId) throws Exception {
        return objectMapper.readTree(restTemplate.getForObject("/api/v1/events/" + eventId, String.class));
    }

    private List<String> eventIdsByState(String state) throws Exception {
        JsonNode events = objectMapper.readTree(
                restTemplate.getForObject("/api/v1/events?status=" + state, String.class));
        return textFieldValues(events, "eventId");
    }

    private void forceRetryDue(String eventId) {
        jdbcTemplate.update(
                "UPDATE restore_event SET next_eligible_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)),
                UUID.fromString(eventId));
    }

    private JsonNode circuit(String targetSystem) throws Exception {
        JsonNode circuits = objectMapper.readTree(
                restTemplate.getForObject("/api/v1/circuit-breakers", String.class));
        for (JsonNode circuit : circuits) {
            if (targetSystem.equals(circuit.path("targetSystem").asText())) return circuit;
        }
        throw new AssertionError("Circuit not found for " + targetSystem);
    }

    private JsonNode simulator(String targetSystem) throws Exception {
        return objectMapper.readTree(
                restTemplate.getForObject("/api/v1/simulator/targets/" + targetSystem, String.class));
    }

    private Map<String, JsonNode> nodesByField(JsonNode values, String field) {
        Map<String, JsonNode> nodes = new HashMap<>();
        values.forEach(value -> nodes.put(value.path(field).asText(), value));
        return nodes;
    }

    private List<Integer> intFieldValues(JsonNode values, String field) {
        List<Integer> integers = new ArrayList<>();
        values.forEach(value -> integers.add(value.path(field).asInt()));
        return integers;
    }

    private List<String> textFieldValues(JsonNode values, String field) {
        List<String> textValues = new ArrayList<>();
        values.forEach(value -> textValues.add(value.path(field).asText()));
        return textValues;
    }

    private List<String> states(JsonNode events) {
        List<String> states = new ArrayList<>();
        events.forEach(event -> states.add(event.path("state").asText()));
        return states;
    }

    private List<String> textValues(JsonNode values) {
        List<String> textValues = new ArrayList<>();
        values.forEach(value -> textValues.add(value.asText()));
        return textValues;
    }

    private List<String> steps(JsonNode events) {
        List<String> steps = new ArrayList<>();
        events.forEach(event -> steps.add(event.path("step").asText()));
        return steps;
    }
}
