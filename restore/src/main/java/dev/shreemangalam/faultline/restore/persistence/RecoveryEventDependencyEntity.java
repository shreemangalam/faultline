package dev.shreemangalam.faultline.restore.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "restore_event_dependency", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "prerequisite_event_id"}))
public class RecoveryEventDependencyEntity {
    @Id
    private UUID id;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Column(name = "prerequisite_event_id", nullable = false)
    private UUID prerequisiteEventId;

    protected RecoveryEventDependencyEntity() { }

    public RecoveryEventDependencyEntity(UUID id, UUID eventId, UUID prerequisiteEventId) {
        this.id = id;
        this.eventId = eventId;
        this.prerequisiteEventId = prerequisiteEventId;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getPrerequisiteEventId() { return prerequisiteEventId; }
}