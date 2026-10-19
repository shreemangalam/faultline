package dev.shreemangalam.faultline.restore.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "faultline.security.enabled=true",
        "faultline.security.api-key=test-operator-api-key"
})
@AutoConfigureMockMvc
class ApiSecurityTest {
    private static final String API_KEY = "test-operator-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestRestTemplate rest;

    @ParameterizedTest
    @CsvSource({"SUCCESS,COMPLETED,200", "TEMPORARY_UNAVAILABLE,RETRY_SCHEDULED,503", "VALIDATION_FAILURE,FAILED_FINAL,422"})
    void executesRealHttpWithSecurityEnabled(String behavior, String expectedState, int expectedStatus) {
        String target = "SECURITY_" + UUID.randomUUID().toString().replace("-", "");
        var configured = rest.exchange("/api/v1/simulator/targets/" + target, HttpMethod.PUT,
                authenticated(Map.of("behavior", behavior, "delayMillis", 0, "resetHistory", true)), JsonNode.class);
        assertThat(configured.getStatusCode()).isEqualTo(HttpStatus.OK);

        var created = rest.postForEntity("/api/v1/events", authenticated(Map.of(
                "idempotencyKey", target, "correlationId", target, "targetSystem", target,
                "payloadReference", "demo://security/delivery", "priority", 1)), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String eventId = created.getBody().path("eventId").asText();

        var executed = rest.postForEntity("/api/v1/worker/run-once", authenticated(Map.of(
                "workerId", target, "maxEvents", 10, "leaseSeconds", 60)), JsonNode.class);
        assertThat(executed.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode event = rest.getForObject("/api/v1/events/" + eventId, JsonNode.class);
        assertThat(event.path("state").asText()).isEqualTo(expectedState);
        assertThat(event.path("attempts").path(0).path("httpStatus").asInt()).isEqualTo(expectedStatus);

        var unauthenticated = rest.postForEntity("/api/v1/simulator/targets/" + target + "/deliveries",
                Map.of("eventId", UUID.randomUUID(), "correlationId", target, "payloadReference", "demo://unauthorized"),
                String.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpEntity<?> authenticated(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Faultline-Api-Key", API_KEY);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void publicReadsHaveSecurityHeadersAndMutationsRequireTheOperatorKey() throws Exception {
        mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        String event = request();
        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey realm=\"faultline\""))
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(post("/api/v1/events").header("X-Faultline-Api-Key", "incorrect-api-key-value")
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/events").header("X-Faultline-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isCreated());
    }

    @Test
    void generatedContractMarksOnlyMutationsAsProtected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.operatorApiKey.name").value("X-Faultline-Api-Key"))
                .andExpect(jsonPath("$['paths']['/api/v1/events']['post']['security'][0]['operatorApiKey']").isArray())
                .andExpect(jsonPath("$['paths']['/api/v1/events']['get']['security']").doesNotExist());
    }

    private static String request() {
        return """
                {
                  "idempotencyKey":"security-%s",
                  "correlationId":"security-contract",
                  "targetSystem":"SALESFORCE",
                  "payloadReference":"demo://security/contract",
                  "priority":2,
                  "prerequisiteEventIds":[]
                }
                """.formatted(UUID.randomUUID());
    }
}
