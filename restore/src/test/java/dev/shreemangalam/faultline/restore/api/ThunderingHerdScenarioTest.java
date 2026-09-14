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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ThunderingHerdScenarioTest {
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
    void boundsOutageTrafficToTheAlreadyClaimedBatch() throws Exception {
        ResponseEntity<String> loadedResponse = restTemplate.postForEntity(
                "/api/v1/scenarios/thundering-herd",
                Map.of("eventCount", 100),
                String.class);
        JsonNode loaded = objectMapper.readTree(loadedResponse.getBody());
        String correlationId = loaded.path("correlationId").asText();
        String targetSystem = loaded.path("targetSystem").asText();

        assertThat(loadedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(loaded.path("scenario").asText()).isEqualTo("THUNDERING_HERD");
        assertThat(loaded.path("eventCount").asInt()).isEqualTo(100);
        assertThat(loaded.path("state").asText()).isEqualTo("ELIGIBLE");
        assertThat(loaded.path("downstreamBehavior").asText()).isEqualTo("TEMPORARY_UNAVAILABLE");
        assertThat(count("restore_event", correlationId)).isEqualTo(100);
        assertThat(count("restore_event_audit", correlationId)).isEqualTo(200);

        assertThat(runOnce()).hasSize(10);
        assertThat(runOnce()).isEmpty();

        JsonNode statistics = statistics();
        assertThat(statistics.path("eventCounts").path("ELIGIBLE").asLong()).isEqualTo(90);
        assertThat(statistics.path("eventCounts").path("RETRY_SCHEDULED").asLong()).isEqualTo(10);
        assertThat(statistics.path("attemptCounts").path("TRANSIENT_FAILURE").asLong()).isEqualTo(10);
        assertThat(circuit(targetSystem).path("state").asText()).isEqualTo("OPEN");
        assertThat(simulator(targetSystem).path("requestCount").asLong()).isEqualTo(10);
        assertThat(simulator(targetSystem).path("acceptedCount").asLong()).isZero();
    }

    @Test
    void defaultsToOneHundredEventsAndRejectsOutOfRangeSizes() throws Exception {
        ResponseEntity<String> defaultResponse = restTemplate.postForEntity(
                "/api/v1/scenarios/thundering-herd", Map.of(), String.class);
        assertThat(defaultResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(objectMapper.readTree(defaultResponse.getBody()).path("eventCount").asInt()).isEqualTo(100);

        for (int invalidSize : new int[]{0, 50_001}) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/scenarios/thundering-herd",
                    Map.of("eventCount", invalidSize),
                    String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    private long count(String table, String correlationId) {
        String sql = "SELECT COUNT(*) FROM " + table + " row_data JOIN restore_event event "
                + "ON event.id = " + ("restore_event".equals(table) ? "row_data.id" : "row_data.event_id")
                + " WHERE event.correlation_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, correlationId);
    }

    private JsonNode runOnce() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/worker/run-once",
                Map.of("workerId", "herd-worker", "maxEvents", 10, "leaseSeconds", 30),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode statistics() throws Exception {
        return objectMapper.readTree(restTemplate.getForObject("/api/v1/statistics", String.class));
    }

    private JsonNode circuit(String targetSystem) throws Exception {
        JsonNode circuits = objectMapper.readTree(restTemplate.getForObject("/api/v1/circuit-breakers", String.class));
        for (JsonNode circuit : circuits) {
            if (targetSystem.equals(circuit.path("targetSystem").asText())) return circuit;
        }
        throw new AssertionError("Circuit not found for " + targetSystem);
    }

    private JsonNode simulator(String targetSystem) throws Exception {
        return objectMapper.readTree(restTemplate.getForObject(
                "/api/v1/simulator/targets/" + targetSystem, String.class));
    }
}
