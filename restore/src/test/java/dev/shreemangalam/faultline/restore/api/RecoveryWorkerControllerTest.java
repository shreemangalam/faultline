package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.JsonNode;
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
        jdbcTemplate.update("DELETE FROM restore_event_audit");
        jdbcTemplate.update("DELETE FROM restore_event_dependency");
        jdbcTemplate.update("DELETE FROM restore_event");
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
                .andExpect(jsonPath("$.audits[2].toState").value("IN_PROGRESS"));
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
                .andExpect(jsonPath("$.audits[3].toState").value("IN_PROGRESS"));
    }

    private String createEvent() throws Exception {
        String response = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"lease-%s","correlationId":"order-lease","targetSystem":"SALESFORCE","payloadReference":"s3://restore/lease.json","priority":1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("eventId").asText();
    }

    private String claimRequest(String workerId, int maxEvents, int leaseSeconds) {
        return """
                {"workerId":"%s","maxEvents":%d,"leaseSeconds":%d}
                """.formatted(workerId, maxEvents, leaseSeconds);
    }
}