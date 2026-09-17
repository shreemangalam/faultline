package dev.shreemangalam.faultline.restore.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "restore.stream.timeout=5s",
                "restore.stream.heartbeat-interval=250ms"
        })
class RecoveryStreamControllerTest {
    @LocalServerPort
    private int port;
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
    void streamsCommittedRecoveryStateChangesAcrossTheWorkerLifecycle() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/stream"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<InputStream> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                value -> assertThat(value).startsWith("text/event-stream"));
        assertThat(response.headers().firstValue("X-Accel-Buffering")).contains("no");

        try (InputStream body = response.body();
             BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            Map<String, String> readyEvent = readNamedEventWithin(reader, "stream-ready");
            assertThat(readyEvent.get("event")).isEqualTo("stream-ready");
            assertThat(objectMapper.readTree(readyEvent.get("data")).path("status").asText()).isEqualTo("CONNECTED");

            String correlationId = "stream-order-" + UUID.randomUUID();
            ResponseEntity<String> createdResponse = restTemplate.postForEntity(
                    "/api/v1/events",
                    Map.of(
                            "idempotencyKey", "stream-" + UUID.randomUUID(),
                            "correlationId", correlationId,
                            "targetSystem", "STREAM_TARGET",
                            "payloadReference", "local://stream/event.json",
                            "priority", 1),
                    String.class);
            assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            String createdEventId = objectMapper.readTree(createdResponse.getBody()).path("eventId").asText();

            Map<String, String> streamedEvent = readNamedEventWithin(reader, "recovery-event");
            assertThat(streamedEvent.get("event")).isEqualTo("recovery-event");
            JsonNode change = objectMapper.readTree(streamedEvent.get("data"));
            assertThat(change.path("eventId").asText()).isEqualTo(createdEventId);
            assertThat(change.path("correlationId").asText()).isEqualTo(correlationId);
            assertThat(change.path("targetSystem").asText()).isEqualTo("STREAM_TARGET");
            assertThat(change.path("state").asText()).isEqualTo("ELIGIBLE");
            assertThat(change.path("occurredAt").asText()).isNotBlank();
            assertThat(restTemplate.getForObject("/api/v1/events/" + createdEventId, String.class))
                    .contains("\"state\":\"ELIGIBLE\"");

            ResponseEntity<String> claimResponse = restTemplate.postForEntity(
                    "/api/v1/worker/claims",
                    Map.of("workerId", "stream-worker", "maxEvents", 1, "leaseSeconds", 30),
                    String.class);
            assertThat(claimResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(claimResponse.getBody()).contains(createdEventId);
            JsonNode claimed = objectMapper.readTree(
                    readNamedEventWithin(reader, "recovery-event").get("data"));
            assertThat(claimed.path("eventId").asText()).isEqualTo(createdEventId);
            assertThat(claimed.path("state").asText()).isEqualTo("IN_PROGRESS");

            ResponseEntity<String> completeResponse = restTemplate.postForEntity(
                    "/api/v1/worker/claims/" + createdEventId + "/complete",
                    Map.of("workerId", "stream-worker"),
                    String.class);
            assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode completed = objectMapper.readTree(
                    readNamedEventWithin(reader, "recovery-event").get("data"));
            assertThat(completed.path("eventId").asText()).isEqualTo(createdEventId);
            assertThat(completed.path("state").asText()).isEqualTo("COMPLETED");
        }
    }

    private Map<String, String> readNamedEventWithin(BufferedReader reader, String eventName) throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> event;
                do {
                    event = readEvent(reader);
                } while (!eventName.equals(event.get("event")));
                return event;
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }).get(5, TimeUnit.SECONDS);
    }

    private Map<String, String> readEvent(BufferedReader reader) throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.isEmpty() && !fields.isEmpty()) return fields;
            int separator = line.indexOf(':');
            if (separator < 0) continue;
            String value = line.substring(separator + 1).stripLeading();
            fields.put(line.substring(0, separator), value);
        }
        throw new IOException("SSE connection closed before a complete event arrived.");
    }
}
