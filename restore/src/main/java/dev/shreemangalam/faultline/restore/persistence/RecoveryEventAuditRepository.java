package dev.shreemangalam.faultline.restore.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryEventAuditRepository extends JpaRepository<RecoveryEventAuditEntity, UUID> {
    List<RecoveryEventAuditEntity> findByEventIdOrderByOccurredAtAsc(UUID eventId);
}