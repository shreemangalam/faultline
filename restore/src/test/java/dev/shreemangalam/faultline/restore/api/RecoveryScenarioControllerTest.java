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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private List<String> states(JsonNode events) {
        List<String> states = new ArrayList<>();
        events.forEach(event -> states.add(event.path("state").asText()));
        return states;
    }

    private List<String> steps(JsonNode events) {
        List<String> steps = new ArrayList<>();
        events.forEach(event -> steps.add(event.path("step").asText()));
        return steps;
    }
}
