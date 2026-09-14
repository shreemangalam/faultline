package dev.shreemangalam.faultline.restore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RetryBackoffIntegrationTest.FixedJitterConfiguration.class)
class RetryBackoffIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
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
    void persistsExponentialBackoffWithInjectedJitter() throws Exception {
        String eventId = create();
        mockMvc.perform(post("/api/v1/worker/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"jitter-worker\",\"maxEvents\":1,\"leaseSeconds\":60}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/worker/claims/{eventId}/retry", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"jitter-worker\",\"reason\":\"target unavailable\"}"))
                .andExpect(status().isOk());
        JsonNode event = objectMapper.readTree(mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        Instant finishedAt = Instant.parse(event.path("attempts").path(0).path("finishedAt").asText());
        Instant nextEligibleAt = Instant.parse(event.path("nextEligibleAt").asText());
        assertThat(Duration.between(finishedAt, nextEligibleAt).toMillis()).isEqualTo(5_750);
    }

    private String create() throws Exception {
        String response = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"jitter-%s","correlationId":"jitter-order","targetSystem":"SALESFORCE","payloadReference":"local://jitter/payload.json","priority":1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("eventId").asText();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedJitterConfiguration {
        @Bean
        @Primary
        RetryJitterSource fixedRetryJitterSource() {
            return upperBoundExclusive -> 750;
        }
    }
}
