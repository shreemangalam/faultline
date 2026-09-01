package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.service.CreateRecoveryEventCommand;
import dev.shreemangalam.faultline.restore.service.RecoveryEventDetails;
import dev.shreemangalam.faultline.restore.service.RecoveryEventService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecoveryEventController {

    private final RecoveryEventService recoveryEventService;

    public RecoveryEventController(RecoveryEventService recoveryEventService) {
        this.recoveryEventService = recoveryEventService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecoveryEventResponse> submit(@Valid @RequestBody CreateRecoveryEventRequest request) {
        var command = new CreateRecoveryEventCommand(
                request.idempotencyKey(), request.correlationId(), request.targetSystem(), request.payloadReference(),
                request.priority(), request.deadlineAt(), request.prerequisiteEventIds());
        var result = recoveryEventService.submit(command);
        RecoveryEventResponse response = toResponse(result.details());
        if (!result.created()) return ResponseEntity.ok(response);
        return ResponseEntity.created(URI.create("/api/v1/events/" + response.eventId())).body(response);
    }

    @GetMapping("/{eventId}")
    public RecoveryEventResponse get(@PathVariable UUID eventId) {
        return toResponse(recoveryEventService.get(eventId));
    }

    @GetMapping
    public List<RecoveryEventResponse> list(@RequestParam(required = false) RecoveryEventState status) {
        return recoveryEventService.list(status).stream().map(this::toResponse).toList();
    }

    private RecoveryEventResponse toResponse(RecoveryEventDetails details) {
        var event = details.event();
        return new RecoveryEventResponse(
                event.getId(), event.getIdempotencyKey(), event.getCorrelationId(), event.getTargetSystem(),
                event.getPayloadReference(), event.getPriority(), event.getDeadlineAt(), event.getState(),
                event.getAttemptCount(), event.getNextEligibleAt(), event.getLeaseOwner(), event.getLeaseExpiresAt(), event.getCreatedAt(), details.prerequisiteEventIds(),
                details.audits().stream().map(audit -> new RecoveryEventAuditResponse(
                        audit.getId(), audit.getToState(), audit.getReason(), audit.getOccurredAt())).toList(),
                details.attempts().stream().map(attempt -> new RecoveryAttemptResponse(
                        attempt.getId(), attempt.getAttemptNumber(), attempt.getWorkerId(), attempt.getOutcome(),
                        attempt.getFailureReason(), attempt.getStartedAt(), attempt.getFinishedAt())).toList());
    }
}