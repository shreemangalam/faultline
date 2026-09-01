package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecoveryEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void persistsAnEligibleEventWithLifecycleAudit() throws Exception {
        String idempotencyKey = "submit-" + UUID.randomUUID();
        String response = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(idempotencyKey, "s3://restore/order-42.json")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/events/")))
                .andExpect(jsonPath("$.state").value("ELIGIBLE"))
                .andExpect(jsonPath("$.attemptCount").value(0))
                .andExpect(jsonPath("$.attempts.length()").value(0))
                .andExpect(jsonPath("$.audits.length()").value(2))
                .andExpect(jsonPath("$.audits[0].toState").value("RECEIVED"))
                .andExpect(jsonPath("$.audits[1].toState").value("ELIGIBLE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        mockMvc.perform(get("/api/v1/events/{eventId}", body.path("eventId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey))
                .andExpect(jsonPath("$.targetSystem").value("SALESFORCE"));
    }

    @Test
    void returnsExistingEventForTheSameIdempotentSubmissionAndRejectsDifferentInput() throws Exception {
        String idempotencyKey = "duplicate-" + UUID.randomUUID();
        String first = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(request(idempotencyKey, "s3://restore/original.json")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(first).path("eventId").asText();

        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(request(idempotencyKey, "s3://restore/original.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId));
        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(request(idempotencyKey, "s3://restore/changed.json")))
                .andExpect(status().isConflict());
    }

    @Test
    void blocksAChildUntilPersistedPrerequisitesComplete() throws Exception {
        String correlationId = "order-dependency-" + UUID.randomUUID();
        String parent = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content(request("parent-" + UUID.randomUUID(), correlationId, "s3://restore/parent.json")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String parentId = objectMapper.readTree(parent).path("eventId").asText();

        String child = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content(dependentRequest("child-" + UUID.randomUUID(), correlationId, parentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("BLOCKED"))
                .andExpect(jsonPath("$.prerequisiteEventIds[0]").value(parentId))
                .andExpect(jsonPath("$.audits[1].toState").value("BLOCKED"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/events/{eventId}", objectMapper.readTree(child).path("eventId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("BLOCKED"))
                .andExpect(jsonPath("$.prerequisiteEventIds.length()").value(1));
    }

    @Test
    void rejectsPrerequisitesFromAnotherCorrelation() throws Exception {
        String parent = mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content(request("foreign-parent-" + UUID.randomUUID(), "order-a", "s3://restore/foreign-parent.json")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON)
                        .content(dependentRequest("foreign-child-" + UUID.randomUUID(), "order-b", objectMapper.readTree(parent).path("eventId").asText())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersTheOperationalQueueByState() throws Exception {
        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(request("queue-" + UUID.randomUUID(), "s3://restore/queue.json")))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/events").param("status", "ELIGIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("ELIGIBLE"));
    }

    private String request(String idempotencyKey, String payloadReference) {
        return request(idempotencyKey, "order-42", payloadReference);
    }

    private String request(String idempotencyKey, String correlationId, String payloadReference) {
        return """
                {"idempotencyKey":"%s","correlationId":"%s","targetSystem":"SALESFORCE","payloadReference":"%s","priority":1}
                """.formatted(idempotencyKey, correlationId, payloadReference);
    }

    private String dependentRequest(String idempotencyKey, String correlationId, String prerequisiteEventId) {
        return """
                {"idempotencyKey":"%s","correlationId":"%s","targetSystem":"LEGACY_WMS","payloadReference":"s3://restore/child.json","priority":1,"prerequisiteEventIds":["%s"]}
                """.formatted(idempotencyKey, correlationId, prerequisiteEventId);
    }
}