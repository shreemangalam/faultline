package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shreemangalam.faultline.restore.service.OperationsDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "restore.operations-demo.enabled=true")
class OperationsDemoTest {
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void clear() {
        for (String table : new String[]{"restore_event_attempt", "restore_event_audit",
                "restore_event_dependency", "restore_event", "restore_circuit_breaker"})
            jdbc.update("DELETE FROM " + table);
    }

    @Test void buildsRealEvidenceAndReleasesDependenciesAfterRecovery() throws Exception {
        var response = http.postForEntity("/api/v1/scenarios/operations-demo", null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode stats = get("/statistics");
        assertThat(stats.path("totalEvents").asInt()).isEqualTo(39);
        assertThat(stats.path("pendingEventCount").asInt()).isEqualTo(24);
        assertThat(stats.path("eventCounts").path("COMPLETED").asInt()).isEqualTo(13);
        assertThat(stats.path("eventCounts").path("BLOCKED").asInt()).isEqualTo(20);
        assertThat(stats.path("eventCounts").path("RETRY_SCHEDULED").asInt()).isEqualTo(3);
        assertThat(stats.path("eventCounts").path("FAILED_FINAL").asInt()).isEqualTo(1);
        assertThat(stats.path("eventCounts").path("FAILED_DEPENDENCY").asInt()).isEqualTo(1);
        assertThat(stats.path("totalAttempts").asInt()).isEqualTo(18);
        assertThat(stats.path("p95SuccessfulRecoveryLatencyMillis").asLong()).isGreaterThanOrEqualTo(5000);
        JsonNode events = get("/events");
        JsonNode root = null;
        for (JsonNode event : events) {
            if (event.path("correlationId").asText().equals("ORD-2026-10479")) {
                assertThat(event.path("state").asText()).isEqualTo("COMPLETED");
                assertThat(event.path("attempts").size()).isEqualTo(2);
                assertThat(event.path("attempts").get(0).path("httpStatus").asInt()).isEqualTo(503);
                assertThat(event.path("attempts").get(1).path("httpStatus").asInt()).isEqualTo(200);
            }
            if (event.path("correlationId").asText().equals("ORD-2026-10482")
                    && event.path("targetSystem").asText().equals("SALESFORCE")) root = event;
        }
        assertThat(root).isNotNull();
        assertThat(root.path("priority").asInt()).isEqualTo(1);
        assertThat(root.path("deadlineAt").asText()).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT state FROM restore_circuit_breaker WHERE target_system='SALESFORCE'", String.class)).isEqualTo("OPEN");
        assertThat(http.postForEntity("/api/v1/scenarios/operations-demo", null, String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(get("/statistics").path("totalEvents").asInt()).isEqualTo(39);
        http.postForEntity("/api/v1/scenarios/operations-demo/recover", null, String.class);
        // Advance only test eligibility; the shipped loader never edits persisted timestamps.
        jdbc.update("UPDATE restore_circuit_breaker SET open_until = CURRENT_TIMESTAMP WHERE target_system='SALESFORCE'");
        jdbc.update("UPDATE restore_event SET next_eligible_at = CURRENT_TIMESTAMP WHERE state='RETRY_SCHEDULED'");
        JsonNode batch = mapper.readTree(http.postForObject("/api/v1/worker/run-once",
                Map.of("workerId", "demo-test", "maxEvents", 5, "leaseSeconds", 60), String.class));
        assertThat(batch.size()).isEqualTo(1); // Exactly one half-open probe.
        assertThat(batch.get(0).path("releasedEventIds").size()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT state FROM restore_circuit_breaker WHERE target_system='SALESFORCE'", String.class)).isEqualTo("CLOSED");
        assertThat(get("/statistics").path("eventCounts").path("BLOCKED").asInt()).isEqualTo(18);
    }

    @Test void refusesAnyNonemptyDatabaseWithoutChangingIt() {
        jdbc.update("""
            INSERT INTO restore_event (id, idempotency_key, request_fingerprint, correlation_id,
            target_system, payload_reference, priority, state, attempt_count, next_eligible_at, created_at, updated_at)
            VALUES (RANDOM_UUID(), 'preserved', 'fingerprint', 'preserved', 'PRESERVED',
            'demo://preserved', 1, 'ELIGIBLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);
        assertThat(http.postForEntity("/api/v1/scenarios/operations-demo", null, String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM restore_event", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM restore_event_attempt", Integer.class)).isZero();
    }

    @Test void disabledLoaderRejectsBeforeAccessingData() {
        var repository = mock(dev.shreemangalam.faultline.restore.persistence.RecoveryEventRepository.class);
        var service = new OperationsDemoService(null, repository, null, null, null, false);
        assertThatThrownBy(service::load).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(service::recover).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(repository);
    }

    private JsonNode get(String path) throws Exception {
        return mapper.readTree(http.getForObject("/api/v1" + path, String.class));
    }
}
