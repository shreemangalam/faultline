package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.simulator.DownstreamSimulatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DownstreamSimulatorController.class)
@Import(DownstreamSimulatorService.class)
class DownstreamSimulatorControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsEachIdempotencyKeyOnlyOnce() throws Exception {
        configure("SALESFORCE", "SUCCESS", 0);
        String eventId = UUID.randomUUID().toString();

        deliver("SALESFORCE", eventId, eventId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.receiptId").isNotEmpty());
        deliver("SALESFORCE", eventId, eventId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.outcome").value("ALREADY_ACCEPTED"));

        mockMvc.perform(get("/api/v1/simulator/targets/SALESFORCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestCount").value(2))
                .andExpect(jsonPath("$.acceptedCount").value(1));
    }

    @Test
    void exposesTransientRateLimitAndPermanentValidationBehaviors() throws Exception {
        String eventId = UUID.randomUUID().toString();
        configure("WMS", "TEMPORARY_UNAVAILABLE", 0);
        deliver("WMS", eventId, eventId)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.outcome").value("TEMPORARY_UNAVAILABLE"));

        configure("WMS", "RATE_LIMITED", 0);
        deliver("WMS", eventId, eventId)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.outcome").value("RATE_LIMITED"));

        configure("WMS", "VALIDATION_FAILURE", 0);
        deliver("WMS", eventId, eventId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.outcome").value("VALIDATION_FAILURE"));
    }

    @Test
    void supportsAControlledDelayedSuccess() throws Exception {
        configure("ERP", "DELAYED_SUCCESS", 10);
        String eventId = UUID.randomUUID().toString();

        deliver("ERP", eventId, eventId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ACCEPTED"));
    }

    private void configure(String targetSystem, String behavior, long delayMillis) throws Exception {
        mockMvc.perform(put("/api/v1/simulator/targets/{targetSystem}", targetSystem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"behavior":"%s","delayMillis":%d}
                                """.formatted(behavior, delayMillis)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.behavior").value(behavior))
                .andExpect(jsonPath("$.requestCount").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions deliver(String targetSystem, String idempotencyKey, String eventId) throws Exception {
        return mockMvc.perform(post("/api/v1/simulator/targets/{targetSystem}/deliveries", targetSystem)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"eventId":"%s","correlationId":"order-simulator","payloadReference":"local://payload.json"}
                        """.formatted(eventId)));
    }
}
