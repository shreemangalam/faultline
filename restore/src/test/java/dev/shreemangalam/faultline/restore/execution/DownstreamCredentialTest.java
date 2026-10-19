package dev.shreemangalam.faultline.restore.execution;

import com.sun.net.httpserver.HttpServer;
import dev.shreemangalam.faultline.restore.service.ClaimedRecoveryEvent;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DownstreamCredentialTest {
    @Test
    void configuredDownstreamsReceiveOnlyTheirExplicitCredential() throws Exception {
        AtomicReference<String> receivedKey = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/simulator/targets/TARGET/deliveries", exchange -> {
            receivedKey.set(exchange.getRequestHeaders().getFirst("X-Faultline-Api-Key"));
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("faultline.security.api-key", "private-local-operator-key")
                    .withProperty("restore.downstream.base-url", "http://127.0.0.1:" + server.getAddress().getPort());
            DownstreamDeliveryClient client = new DownstreamDeliveryClient(RestClient.builder(), environment,
                    Duration.ofSeconds(1), Duration.ofSeconds(2));
            ClaimedRecoveryEvent event = new ClaimedRecoveryEvent(UUID.randomUUID(), "credential-test", "TARGET",
                    "demo://credential", 1, 1, "credential-worker", Instant.now().plusSeconds(60));
            assertThat(client.deliver(event).classification()).isEqualTo(DeliveryClassification.SUCCESS);
            assertThat(receivedKey.get()).isNull();

            environment.withProperty("restore.downstream.api-key", "explicit-target-credential");
            assertThat(client.deliver(event).classification()).isEqualTo(DeliveryClassification.SUCCESS);
            assertThat(receivedKey.get()).isEqualTo("explicit-target-credential");
        } finally {
            server.stop(0);
        }
    }
}
