package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventEntity;
import dev.shreemangalam.faultline.restore.simulator.DownstreamBehavior;
import dev.shreemangalam.faultline.restore.simulator.DownstreamSimulatorService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RecoveryScenarioService {
    private static final int PERSISTENCE_BATCH_SIZE = 500;

    private final RecoveryEventService eventService;
    private final CircuitBreakerService circuitBreakerService;
    private final DownstreamSimulatorService simulatorService;
    private final EntityManager entityManager;
    private final Clock clock;

    public RecoveryScenarioService(
            RecoveryEventService eventService,
            CircuitBreakerService circuitBreakerService,
            DownstreamSimulatorService simulatorService,
            EntityManager entityManager,
            Clock clock) {
        this.eventService = eventService;
        this.circuitBreakerService = circuitBreakerService;
        this.simulatorService = simulatorService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public DependencyCascadeScenario loadDependencyCascade() {
        UUID scenarioId = UUID.randomUUID();
        String correlationId = "dependency-cascade-" + scenarioId;
        String targetSuffix = scenarioId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        List<ScenarioRecoveryEvent> events = new ArrayList<>();

        UUID customerValidated = submit(
                events, scenarioId, correlationId, "CUSTOMER_VALIDATED", "CRM_" + targetSuffix, 1, List.of());
        UUID orderCreated = submit(
                events, scenarioId, correlationId, "ORDER_CREATED", "ERP_" + targetSuffix, 1,
                List.of(customerValidated));
        UUID inventoryReserved = submit(
                events, scenarioId, correlationId, "INVENTORY_RESERVED", "WMS_" + targetSuffix, 1,
                List.of(orderCreated));
        UUID paymentCaptured = submit(
                events, scenarioId, correlationId, "PAYMENT_CAPTURED", "PAYMENTS_" + targetSuffix, 2,
                List.of(orderCreated));
        UUID shipmentScheduled = submit(
                events, scenarioId, correlationId, "SHIPMENT_SCHEDULED", "TMS_" + targetSuffix, 1,
                List.of(inventoryReserved));
        submit(
                events, scenarioId, correlationId, "ORDER_CONFIRMED", "ERP_" + targetSuffix, 1,
                List.of(paymentCaptured, shipmentScheduled));

        return new DependencyCascadeScenario(scenarioId, correlationId, List.copyOf(events));
    }

    @Transactional
    public ThunderingHerdScenario loadThunderingHerd(int eventCount) {
        UUID scenarioId = UUID.randomUUID();
        String correlationId = "thundering-herd-" + scenarioId;
        String targetSystem = "OUTAGE_" + scenarioId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Instant now = clock.instant();
        simulatorService.configure(targetSystem, DownstreamBehavior.TEMPORARY_UNAVAILABLE, 0, true);
        circuitBreakerService.ensureExists(targetSystem, now);

        for (int index = 0; index < eventCount; index++) {
            String eventKey = "herd-" + scenarioId + "-" + index;
            var command = new CreateRecoveryEventCommand(
                    eventKey,
                    correlationId,
                    targetSystem,
                    "scenario://thundering-herd/" + index,
                    1,
                    null,
                    List.of());
            RecoveryEventEntity event = RecoveryEventEntity.receive(
                    UUID.randomUUID(),
                    eventKey,
                    RecoveryEventFingerprint.calculate(command),
                    correlationId,
                    targetSystem,
                    command.payloadReference(),
                    command.priority(),
                    null,
                    RecoveryEventState.ELIGIBLE,
                    now);
            entityManager.persist(event);
            entityManager.persist(new RecoveryEventAuditEntity(
                    UUID.randomUUID(), event.getId(), RecoveryEventState.RECEIVED,
                    "Event accepted for recovery.", now));
            entityManager.persist(new RecoveryEventAuditEntity(
                    UUID.randomUUID(), event.getId(), RecoveryEventState.ELIGIBLE,
                    "No incomplete prerequisites are registered.", now));

            if ((index + 1) % PERSISTENCE_BATCH_SIZE == 0 || index + 1 == eventCount) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        return new ThunderingHerdScenario(
                scenarioId,
                correlationId,
                targetSystem,
                eventCount,
                RecoveryEventState.ELIGIBLE,
                DownstreamBehavior.TEMPORARY_UNAVAILABLE,
                now);
    }

    private UUID submit(
            List<ScenarioRecoveryEvent> events,
            UUID scenarioId,
            String correlationId,
            String step,
            String targetSystem,
            int priority,
            List<UUID> prerequisites) {
        var submission = eventService.submit(new CreateRecoveryEventCommand(
                "dependency-" + scenarioId + "-" + step.toLowerCase(Locale.ROOT).replace('_', '-'),
                correlationId,
                targetSystem,
                "scenario://dependency-cascade/" + step.toLowerCase(Locale.ROOT).replace('_', '-'),
                priority,
                null,
                prerequisites));
        var event = submission.details().event();
        events.add(new ScenarioRecoveryEvent(
                step, event.getId(), event.getTargetSystem(), event.getState(), prerequisites));
        return event.getId();
    }
}
