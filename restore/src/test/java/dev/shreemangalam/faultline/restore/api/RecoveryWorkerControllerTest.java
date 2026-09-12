package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecoveryWorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEvents() {
        jdbcTemplate.update("DELETE FROM restore_event_attempt");
        jdbcTemplate.update("DELETE FROM restore_event_audit");
        jdbcTemplate.update("DELETE FROM restore_event_dependency");
        jdbcTemplate.update("DELETE FROM restore_event");
        jdbcTemplate.update("DELETE FROM restore_circuit_breaker");
    }

    @Test
    void claimsDueEligibleEventsWithAnAuditedVisibilityLease() throws Exception {
        String eventId = createEvent();

        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-a", 1, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(eventId))
                .andExpect(jsonPath("$[0].attemptCount").value(1))
                .andExpect(jsonPath("$[0].leaseOwner").value("worker-a"))
                .andExpect(jsonPath("$[0].leaseExpiresAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.leaseOwner").value("worker-a"))
                .andExpect(jsonPath("$.audits.length()").value(3))
                .andExpect(jsonPath("$.audits[2].toState").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.attempts.length()").value(1))
                .andExpect(jsonPath("$.attempts[0].outcome").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.attempts[0].workerId").value("worker-a"));
    }

    @Test
    void reclaimsAnExpiredVisibilityLeaseForAnotherWorker() throws Exception {
        String eventId = createEvent();
        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-a", 1, 60)))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE restore_event SET lease_expires_at = ? WHERE id = ?", Timestamp.from(Instant.now().minusSeconds(1)), UUID.fromString(eventId));

        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-b", 1, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(eventId))
                .andExpect(jsonPath("$[0].attemptCount").value(2))
                .andExpect(jsonPath("$[0].leaseOwner").value("worker-b"));

        mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.attemptCount").value(2))
                .andExpect(jsonPath("$.leaseOwner").value("worker-b"))
                .andExpect(jsonPath("$.audits.length()").value(4))
                .andExpect(jsonPath("$.audits[3].toState").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.attempts.length()").value(2))
                .andExpect(jsonPath("$.attempts[0].outcome").value("LEASE_EXPIRED"))
                .andExpect(jsonPath("$.attempts[1].outcome").value("IN_PROGRESS"));
    }

    @Test
    void completesOwnedLeasesAndReleasesOnlyAfterEveryPrerequisiteCompletes() throws Exception {
        String correlationId = "order-release-" + UUID.randomUUID();
        String firstParentId = createEvent(correlationId);
        String secondParentId = createEvent(correlationId);
        String childId = createDependentEvent(correlationId, firstParentId, secondParentId);

        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-a", 2, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/api/v1/worker/claims/{eventId}/complete", firstParentId).contentType(MediaType.APPLICATION_JSON)
                        .content(completeRequest("worker-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(firstParentId))
                .andExpect(jsonPath("$.releasedEventIds.length()").value(0));
        mockMvc.perform(get("/api/v1/events/{eventId}", firstParentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempts[0].outcome").value("SUCCEEDED"))
                .andExpect(jsonPath("$.attempts[0].finishedAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/events/{eventId}", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("BLOCKED"));

        mockMvc.perform(post("/api/v1/worker/claims/{eventId}/complete", secondParentId).contentType(MediaType.APPLICATION_JSON)
                        .content(completeRequest("worker-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(secondParentId))
                .andExpect(jsonPath("$.releasedEventIds[0]").value(childId));
        mockMvc.perform(get("/api/v1/events/{eventId}", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ELIGIBLE"))
                .andExpect(jsonPath("$.audits.length()").value(3))
                .andExpect(jsonPath("$.audits[2].toState").value("ELIGIBLE"));
    }

    @Test
    void terminalPrerequisiteFailurePropagatesToExistingAndNewDependants() throws Exception {
        String correlationId = "order-terminal-" + UUID.randomUUID();
        String rootId = createEvent(correlationId);
        String childId = createDependentEventWithState(correlationId, "BLOCKED", rootId);
        String grandchildId = createDependentEventWithState(correlationId, "BLOCKED", childId);

        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-a", 1, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(rootId));

        mockMvc.perform(post("/api/v1/worker/claims/{eventId}/fail-final", rootId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failRequest("worker-a", "root payload is invalid")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("FAILED_FINAL"));

        mockMvc.perform(get("/api/v1/events/{eventId}", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("FAILED_DEPENDENCY"))
                .andExpect(jsonPath("$.attempts.length()").value(0))
                .andExpect(jsonPath("$.audits[2].toState").value("FAILED_DEPENDENCY"))
                .andExpect(jsonPath("$.audits[2].reason").value(org.hamcrest.Matchers.containsString(rootId)));
        mockMvc.perform(get("/api/v1/events/{eventId}", grandchildId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("FAILED_DEPENDENCY"))
                .andExpect(jsonPath("$.audits[2].reason").value(org.hamcrest.Matchers.containsString(childId)));

        String lateChildId = createDependentEventWithState(correlationId, "FAILED_DEPENDENCY", rootId);
        mockMvc.perform(get("/api/v1/events/{eventId}", lateChildId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audits[1].reason").value(org.hamcrest.Matchers.containsString(rootId)));

        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-b", 10, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recordsPermanentFailuresWithoutRetryingOrOpeningTheCircuit() throws Exception {
        String eventId = createEvent();
        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-a", 1, 60)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/worker/claims/{eventId}/fail-final", eventId).contentType(MediaType.APPLICATION_JSON)
                        .content(failRequest("worker-a", "payload violates downstream schema")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.state").value("FAILED_FINAL"));

        mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("FAILED_FINAL"))
                .andExpect(jsonPath("$.leaseOwner").isEmpty())
                .andExpect(jsonPath("$.attempts[0].outcome").value("PERMANENT_FAILURE"))
                .andExpect(jsonPath("$.attempts[0].failureReason").value("payload violates downstream schema"));
        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-b", 1, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        Integer failures = jdbcTemplate.queryForObject(
                "SELECT consecutive_failures FROM restore_circuit_breaker WHERE target_system = 'SALESFORCE'", Integer.class);
        org.assertj.core.api.Assertions.assertThat(failures).isZero();
    }

    @Test
    void rejectsCompletionFromAWorkerThatDoesNotOwnTheLease() throws Exception {
        String eventId = createEvent();
        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimRequest("worker-a", 1, 60)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/worker/claims/{eventId}/complete", eventId).contentType(MediaType.APPLICATION_JSON)
                        .content(completeRequest("worker-b")))
                .andExpect(status().isConflict());
    }

    private String createEvent() throws Exception {
        return createEvent("order-lease");
    }

    private String createEvent(String correlationId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"lease-%s","correlationId":"%s","targetSystem":"SALESFORCE","payloadReference":"s3://restore/lease.json","priority":1}
                                """.formatted(UUID.randomUUID(), correlationId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("eventId").asText();
    }

    private String createDependentEvent(String correlationId, String firstParentId, String secondParentId) throws Exception {
        return createDependentEventWithState(correlationId, "BLOCKED", firstParentId, secondParentId);
    }

    private String createDependentEventWithState(
            String correlationId,
            String expectedState,
            String... prerequisiteEventIds) throws Exception {
        String prerequisiteJson = "\"" + String.join("\",\"", prerequisiteEventIds) + "\"";
        String response = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"child-%s","correlationId":"%s","targetSystem":"LEGACY_WMS","payloadReference":"s3://restore/child.json","priority":1,"prerequisiteEventIds":[%s]}
                                """.formatted(UUID.randomUUID(), correlationId, prerequisiteJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value(expectedState))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("eventId").asText();
    }

    private String claimRequest(String workerId, int maxEvents, int leaseSeconds) {
        return """
                {"workerId":"%s","maxEvents":%d,"leaseSeconds":%d}
                """.formatted(workerId, maxEvents, leaseSeconds);
    }

    private String failRequest(String workerId, String reason) {
        return """
                {"workerId":"%s","reason":"%s"}
                """.formatted(workerId, reason);
    }

    private String completeRequest(String workerId) {
        return """
                {"workerId":"%s"}
                """.formatted(workerId);
    }
}