package dev.shreemangalam.faultline.restore.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryEventDependencyRepository extends JpaRepository<RecoveryEventDependencyEntity, UUID> {
    List<RecoveryEventDependencyEntity> findByEventIdOrderByPrerequisiteEventIdAsc(UUID eventId);
    List<RecoveryEventDependencyEntity> findByPrerequisiteEventIdOrderByEventIdAsc(UUID prerequisiteEventId);
}