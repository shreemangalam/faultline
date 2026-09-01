package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryEventRepository extends JpaRepository<RecoveryEventEntity, UUID> {
    Optional<RecoveryEventEntity> findByIdempotencyKey(String idempotencyKey);
    List<RecoveryEventEntity> findByStateOrderByCreatedAtAsc(RecoveryEventState state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT recoveryEvent FROM RecoveryEventEntity recoveryEvent WHERE recoveryEvent.id = :eventId")
    Optional<RecoveryEventEntity> lockById(@Param("eventId") UUID eventId);

    @Query(value = """
            SELECT * FROM restore_event
            WHERE (state IN ('ELIGIBLE', 'RETRY_SCHEDULED') AND next_eligible_at <= :now)
               OR (state = 'IN_PROGRESS' AND lease_expires_at IS NOT NULL AND lease_expires_at <= :now)
            ORDER BY priority ASC, deadline_at ASC NULLS LAST, attempt_count ASC, created_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<RecoveryEventEntity> lockClaimable(@Param("now") Instant now, @Param("batchSize") int batchSize);
}