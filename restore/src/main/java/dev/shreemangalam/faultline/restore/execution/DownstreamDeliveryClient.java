package dev.shreemangalam.faultline.restore.execution;

import dev.shreemangalam.faultline.restore.service.ClaimedRecoveryEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class DownstreamDeliveryClient {
    private final RestClient restClient;
    private final Environment environment;

    public DownstreamDeliveryClient(
            RestClient.Builder builder,
            Environment environment,
            @Value("${restore.downstream.connect-timeout:1s}") Duration connectTimeout,
            @Value("${restore.downstream.read-timeout:2s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.requestFactory(requestFactory).build();
        this.environment = environment;
    }

    public DownstreamExecutionResult deliver(ClaimedRecoveryEvent event) {
        long startedAt = System.nanoTime();
        try {
            return restClient.post()
                    .uri(baseUrl() + "/api/v1/simulator/targets/{targetSystem}/deliveries", event.targetSystem())
                    .header("Idempotency-Key", event.eventId().toString())
                    .headers(headers -> {
                        String apiKey = environment.getProperty("restore.downstream.api-key");
                        // Reuse the operator key only for the built-in, same-service simulator.
                        if (!StringUtils.hasText(apiKey)
                                && !StringUtils.hasText(environment.getProperty("restore.downstream.base-url"))) {
                            apiKey = environment.getProperty("faultline.security.api-key");
                        }
                        if (StringUtils.hasText(apiKey)) headers.set("X-Faultline-Api-Key", apiKey);
                    })
                    .body(new DownstreamDeliveryPayload(event.eventId(), event.correlationId(), event.payloadReference()))
                    .exchange((request, response) -> {
                        StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        return classify(response.getStatusCode().value(), elapsedMillis(startedAt));
                    });
        } catch (ResourceAccessException exception) {
            return new DownstreamExecutionResult(
                    DeliveryClassification.TRANSIENT_FAILURE, null, elapsedMillis(startedAt),
                    "Downstream request timed out or could not connect.");
        } catch (RestClientException exception) {
            return new DownstreamExecutionResult(
                    DeliveryClassification.TRANSIENT_FAILURE, null, elapsedMillis(startedAt),
                    "Downstream HTTP exchange failed.");
        }
    }

    private DownstreamExecutionResult classify(int status, long durationMillis) {
        if (status >= 200 && status < 300) {
            return new DownstreamExecutionResult(DeliveryClassification.SUCCESS, status, durationMillis, "Downstream accepted the event.");
        }
        if (status == 408 || status == 425 || status == 429 || status >= 500) {
            return new DownstreamExecutionResult(
                    DeliveryClassification.TRANSIENT_FAILURE, status, durationMillis,
                    "Downstream returned retriable HTTP " + status + ".");
        }
        return new DownstreamExecutionResult(
                DeliveryClassification.PERMANENT_FAILURE, status, durationMillis,
                "Downstream returned non-retriable HTTP " + status + ".");
    }

    private String baseUrl() {
        String configured = environment.getProperty("restore.downstream.base-url");
        if (StringUtils.hasText(configured)) return configured.replaceAll("/+$", "");
        Integer localPort = environment.getProperty("local.server.port", Integer.class);
        int port = localPort != null ? localPort : environment.getProperty("server.port", Integer.class, 8082);
        return "http://127.0.0.1:" + port;
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    }
}
