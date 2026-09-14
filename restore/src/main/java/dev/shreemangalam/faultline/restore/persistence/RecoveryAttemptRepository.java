package dev.shreemangalam.faultline.restore.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryAttemptRepository extends JpaRepository<RecoveryAttemptEntity, UUID> {
    Optional<RecoveryAttemptEntity> findByEventIdAndFinishedAtIsNull(UUID eventId);
    List<RecoveryAttemptEntity> findByEventIdOrderByAttemptNumberAsc(UUID eventId);

    @Query("SELECT attempt.outcome AS outcome, COUNT(attempt) AS count FROM RecoveryAttemptEntity attempt GROUP BY attempt.outcome")
    List<RecoveryAttemptOutcomeCount> countByOutcome();
}
