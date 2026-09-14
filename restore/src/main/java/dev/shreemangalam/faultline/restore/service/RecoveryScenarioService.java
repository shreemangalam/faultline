package dev.shreemangalam.faultline.restore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RecoveryScenarioService {
    private final RecoveryEventService eventService;

    public RecoveryScenarioService(RecoveryEventService eventService) {
        this.eventService = eventService;
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
