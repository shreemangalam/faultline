package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.service.ClaimRecoveryEventsCommand;
import dev.shreemangalam.faultline.restore.service.RecoveryEventLeaseService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/worker", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecoveryWorkerController {

    private final RecoveryEventLeaseService leaseService;

    public RecoveryWorkerController(RecoveryEventLeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @PostMapping(path = "/claims", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ClaimedRecoveryEventResponse> claim(@Valid @RequestBody ClaimRecoveryEventsRequest request) {
        return leaseService.claim(new ClaimRecoveryEventsCommand(request.workerId(), request.maxEvents(), request.leaseSeconds()))
                .stream().map(event -> new ClaimedRecoveryEventResponse(
                        event.eventId(), event.correlationId(), event.targetSystem(), event.payloadReference(),
                        event.priority(), event.attemptCount(), event.leaseOwner(), event.leaseExpiresAt()))
                .toList();
    }
}