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

    @Query("SELECT recoveryEvent.state AS state, COUNT(recoveryEvent) AS count FROM RecoveryEventEntity recoveryEvent GROUP BY recoveryEvent.state")
    List<RecoveryEventStateCount> countByState();

    @Query("SELECT recoveryEvent.createdAt AS createdAt, recoveryEvent.updatedAt AS completedAt FROM RecoveryEventEntity recoveryEvent WHERE recoveryEvent.state = :state")
    List<CompletedRecoveryTiming> findCompletionTimings(@Param("state") RecoveryEventState state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT recoveryEvent FROM RecoveryEventEntity recoveryEvent WHERE recoveryEvent.id = :eventId")
    Optional<RecoveryEventEntity> lockById(@Param("eventId") UUID eventId);

    @Query(value = """
            SELECT event.* FROM restore_event event
            LEFT JOIN (
                SELECT dependency.prerequisite_event_id, COUNT(*) AS blocked_dependant_count
                FROM restore_event_dependency dependency
                JOIN restore_event dependant ON dependant.id = dependency.event_id
                WHERE dependant.state = 'BLOCKED'
                GROUP BY dependency.prerequisite_event_id
            ) fanout ON fanout.prerequisite_event_id = event.id
            WHERE ((event.state IN ('ELIGIBLE', 'RETRY_SCHEDULED') AND event.next_eligible_at <= :now)
               OR (event.state = 'IN_PROGRESS' AND event.lease_expires_at IS NOT NULL AND event.lease_expires_at <= :now))
              AND NOT EXISTS (
                  SELECT 1 FROM restore_circuit_breaker circuit
                  WHERE circuit.target_system = event.target_system
                    AND (circuit.state = 'HALF_OPEN'
                         OR (circuit.state = 'OPEN' AND circuit.open_until > :now))
              )
            ORDER BY event.priority ASC,
                     event.deadline_at ASC NULLS LAST,
                     COALESCE(fanout.blocked_dependant_count, 0) DESC,
                     event.attempt_count ASC,
                     event.created_at ASC,
                     event.id ASC
            LIMIT :batchSize
            FOR UPDATE OF event SKIP LOCKED
            """, nativeQuery = true)
    List<RecoveryEventEntity> lockClaimable(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
