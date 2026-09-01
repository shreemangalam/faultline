package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventAuditRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventEntity;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RecoveryEventLeaseService {

    private final RecoveryEventRepository eventRepository;
    private final RecoveryEventAuditRepository auditRepository;
    private final Clock clock;

    public RecoveryEventLeaseService(RecoveryEventRepository eventRepository, RecoveryEventAuditRepository auditRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    @Transactional
    public List<ClaimedRecoveryEvent> claim(ClaimRecoveryEventsCommand command) {
        Instant now = clock.instant();
        Instant leaseExpiresAt = now.plusSeconds(command.leaseSeconds());
        List<RecoveryEventEntity> events = eventRepository.lockClaimable(now, command.maxEvents());
        List<RecoveryEventAuditEntity> audits = events.stream().map(event -> {
            boolean reclaimed = event.getState() == RecoveryEventState.IN_PROGRESS;
            event.claim(command.workerId(), leaseExpiresAt, now);
            String reason = reclaimed
                    ? "Visibility lease expired; reclaimed by worker " + command.workerId() + "."
                    : "Claimed by worker " + command.workerId() + ".";
            return new RecoveryEventAuditEntity(UUID.randomUUID(), event.getId(), RecoveryEventState.IN_PROGRESS, reason, now);
        }).toList();
        auditRepository.saveAll(audits);
        return events.stream().map(event -> new ClaimedRecoveryEvent(
                event.getId(), event.getCorrelationId(), event.getTargetSystem(), event.getPayloadReference(),
                event.getPriority(), event.getAttemptCount(), event.getLeaseOwner(), event.getLeaseExpiresAt())).toList();
    }
}