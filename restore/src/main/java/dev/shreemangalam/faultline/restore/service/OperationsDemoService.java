package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.execution.ExecuteRecoveryBatchCommand;
import dev.shreemangalam.faultline.restore.execution.RecoveryBatchExecutor;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventRepository;
import dev.shreemangalam.faultline.restore.simulator.DownstreamBehavior;
import dev.shreemangalam.faultline.restore.simulator.DownstreamSimulatorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Opt-in, empty-database walkthrough. Transitions use the production HTTP executor. */
@Service
public class OperationsDemoService {
    private final RecoveryEventService events;
    private final RecoveryEventRepository repository;
    private final RecoveryBatchExecutor executor;
    private final DownstreamSimulatorService simulator;
    private final Clock clock;
    private final boolean enabled;

    public OperationsDemoService(RecoveryEventService events, RecoveryEventRepository repository,
            RecoveryBatchExecutor executor, DownstreamSimulatorService simulator, Clock clock,
            @Value("${restore.operations-demo.enabled:false}") boolean enabled) {
        this.events = events;
        this.repository = repository;
        this.executor = executor;
        this.simulator = simulator;
        this.clock = clock;
        this.enabled = enabled;
    }

    public record Status(boolean enabled, long eventCount) { }
    public Status status() { return new Status(enabled, repository.count()); }

    public synchronized Status load() throws InterruptedException {
        requireEnabled();
        if (repository.count() != 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Operations demo requires an empty, isolated database. Existing records were preserved.");

        simulator.configure("SAP_S4_FINANCE", DownstreamBehavior.VALIDATION_FAILURE, 0, false);
        UUID malformed = submit("CUS-2026-2081", "customer-update-invalid", "SAP_S4_FINANCE", 2, null, List.of());
        submit("CUS-2026-2081", "customer-confirmation", "CUSTOMER_PORTAL", 2, null, List.of(malformed));
        run(1);
        simulator.configure("SAP_S4_FINANCE", DownstreamBehavior.SUCCESS, 0, false);

        String[] targets = {"SAP_S4_FINANCE", "BANK_GATEWAY", "CARRIER_API", "CUSTOMER_PORTAL"};
        for (int index = 0; index < 12; index++)
            submit("ORD-2026-" + (10460 + index), "order-settlement", targets[index % targets.length], 3, null, List.of());
        run(10);
        run(2);

        simulator.configure("LEGACY_WMS", DownstreamBehavior.TEMPORARY_UNAVAILABLE, 0, false);
        UUID retried = submit("ORD-2026-10479", "inventory-reservation", "LEGACY_WMS", 2, null, List.of());
        run(1);
        simulator.configure("LEGACY_WMS", DownstreamBehavior.SUCCESS, 0, false);
        // Observe the real backoff, including jitter. Do not rewrite timestamps or eligibility.
        Instant due = repository.findById(retried).orElseThrow().getNextEligibleAt();
        long waitMillis = java.time.Duration.between(clock.instant(), due).toMillis();
        if (waitMillis > 0) Thread.sleep(waitMillis + 25);
        run(1);

        simulator.configure("SALESFORCE", DownstreamBehavior.TEMPORARY_UNAVAILABLE, 0, false);
        for (int index = 0; index < 4; index++) {
            String order = "ORD-2026-" + (10482 + index);
            Instant deadline = clock.instant().plusSeconds(index == 0 ? 900 : 3600);
            UUID root = submit(order, "cpi-order-sync", "SALESFORCE", 1, deadline, List.of());
            UUID inventory = submit(order, "inventory-reservation", "LEGACY_WMS", 2, deadline, List.of(root));
            UUID payment = submit(order, "payment-capture", "BANK_GATEWAY", 2, deadline, List.of(root));
            UUID invoice = submit(order, "invoice-posting", "SAP_S4_FINANCE", 2, deadline, List.of(payment));
            UUID shipment = submit(order, "shipment-booking", "CARRIER_API", 2, deadline, List.of(inventory));
            submit(order, "order-confirmation", "CUSTOMER_PORTAL", 2, deadline, List.of(invoice, shipment));
        }
        run(3); // Three real 503s open SALESFORCE; twenty dependent messages remain blocked.
        return status();
    }

    public Status recover() {
        requireEnabled();
        simulator.configure("SALESFORCE", DownstreamBehavior.SUCCESS, 0, false);
        return status(); // Cooldown and the single probe still govern the next manual batch.
    }

    private void requireEnabled() {
        if (!enabled) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Operations demo is disabled. Start the isolated operations-demo Compose stack.");
    }

    private UUID submit(String correlation, String step, String target, int priority, Instant deadline, List<UUID> prerequisites) {
        return events.submit(new CreateRecoveryEventCommand("operations-demo/" + correlation + "/" + step,
                correlation, target, "demo://operations/" + correlation + "/" + step,
                priority, deadline, prerequisites)).details().event().getId();
    }

    private void run(int count) {
        executor.execute(new ExecuteRecoveryBatchCommand("operations-demo-loader", count, 60));
    }
}
