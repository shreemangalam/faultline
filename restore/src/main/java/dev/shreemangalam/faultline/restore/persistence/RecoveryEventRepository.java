package dev.shreemangalam.faultline.restore.persistence;

import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryEventRepository extends JpaRepository<RecoveryEventEntity, UUID> {
    Optional<RecoveryEventEntity> findByIdempotencyKey(String idempotencyKey);
    List<RecoveryEventEntity> findByStateOrderByCreatedAtAsc(RecoveryEventState state);
}