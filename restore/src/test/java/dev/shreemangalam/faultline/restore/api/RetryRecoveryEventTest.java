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
class RetryRecoveryEventTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clear() {
        jdbcTemplate.update("DELETE FROM restore_event_audit");
        jdbcTemplate.update("DELETE FROM restore_event_dependency");
        jdbcTemplate.update("DELETE FROM restore_event");
        jdbcTemplate.update("DELETE FROM restore_circuit_breaker");
    }

    @Test
    void opensCircuitAfterThreeFailuresAndAllowsOneSuccessfulHalfOpenProbe() throws Exception {
        String eventId = create();
        for (int attempt = 1; attempt <= 3; attempt++) {
            org.assertj.core.api.Assertions.assertThat(claim(1).path(0).path("eventId").asText()).isEqualTo(eventId);
            mockMvc.perform(post("/api/v1/worker/claims/{eventId}/retry", eventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"workerId\":\"retry-worker\",\"reason\":\"temporary downstream outage\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value(attempt == 3 ? "EXHAUSTED" : "RETRY_SCHEDULED"));
            if (attempt < 3) {
                jdbcTemplate.update("UPDATE restore_event SET next_eligible_at = ? WHERE id = ?", Timestamp.from(Instant.now().minusSeconds(1)), UUID.fromString(eventId));
            }
        }

        mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EXHAUSTED"))
                .andExpect(jsonPath("$.attemptCount").value(3));
        mockMvc.perform(get("/api/v1/circuit-breakers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("OPEN"))
                .andExpect(jsonPath("$[0].consecutiveFailures").value(3));

        String firstProbeCandidate = create();
        mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON).content(claimRequest(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        jdbcTemplate.update("UPDATE restore_circuit_breaker SET open_until = ? WHERE target_system = 'SALESFORCE'", Timestamp.from(Instant.now().minusSeconds(1)));
        create();
        JsonNode probes = claim(10);
        org.assertj.core.api.Assertions.assertThat(probes.size()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(probes.path(0).path("eventId").asText()).isEqualTo(firstProbeCandidate);
        mockMvc.perform(get("/api/v1/circuit-breakers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("HALF_OPEN"));

        mockMvc.perform(post("/api/v1/worker/claims/{eventId}/complete", firstProbeCandidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"retry-worker\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/circuit-breakers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("CLOSED"))
                .andExpect(jsonPath("$[0].consecutiveFailures").value(0));
    }

    private JsonNode claim(int maxEvents) throws Exception {
        String response = mockMvc.perform(post("/api/v1/worker/claims").contentType(MediaType.APPLICATION_JSON).content(claimRequest(maxEvents)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String claimRequest(int maxEvents) {
        return "{\"workerId\":\"retry-worker\",\"maxEvents\":" + maxEvents + ",\"leaseSeconds\":60}";
    }

    private String create() throws Exception {
        String response = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"retry-" + UUID.randomUUID() + "\",\"correlationId\":\"retry-order\",\"targetSystem\":\"SALESFORCE\",\"payloadReference\":\"s3://restore/retry.json\",\"priority\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("eventId").asText();
    }
}