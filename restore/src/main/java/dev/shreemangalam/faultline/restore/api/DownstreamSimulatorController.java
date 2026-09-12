package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.simulator.DownstreamSimulatorService;
import dev.shreemangalam.faultline.restore.simulator.SimulatedDelivery;
import dev.shreemangalam.faultline.restore.simulator.SimulatorTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(path = "/api/v1/simulator/targets", produces = MediaType.APPLICATION_JSON_VALUE)
public class DownstreamSimulatorController {
    private final DownstreamSimulatorService service;

    public DownstreamSimulatorController(DownstreamSimulatorService service) {
        this.service = service;
    }

    @PutMapping(path = "/{targetSystem}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SimulatorTargetResponse configure(
            @PathVariable @NotBlank @Size(max = 100) String targetSystem,
            @Valid @RequestBody ConfigureSimulatorTargetRequest request) {
        return response(service.configure(targetSystem, request.behavior(), request.delayMillis()));
    }

    @GetMapping("/{targetSystem}")
    public SimulatorTargetResponse get(@PathVariable @NotBlank @Size(max = 100) String targetSystem) {
        return response(service.get(targetSystem));
    }

    @PostMapping(path = "/{targetSystem}/deliveries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimulatedDeliveryResponse> deliver(
            @PathVariable @NotBlank @Size(max = 100) String targetSystem,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody SimulatedDeliveryRequest request) {
        SimulatedDelivery delivery = service.deliver(targetSystem, idempotencyKey);
        var builder = ResponseEntity.status(delivery.statusCode());
        if (delivery.statusCode() == 429) builder.header(HttpHeaders.RETRY_AFTER, "5");
        return builder.body(new SimulatedDeliveryResponse(
                delivery.accepted(), delivery.duplicate(), delivery.receiptId(), delivery.outcome()));
    }

    private SimulatorTargetResponse response(SimulatorTarget target) {
        return new SimulatorTargetResponse(target.targetSystem(), target.behavior(), target.delayMillis(), target.requestCount(), target.acceptedCount());
    }
}
