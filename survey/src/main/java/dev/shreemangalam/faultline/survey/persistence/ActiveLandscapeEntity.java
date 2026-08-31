package dev.shreemangalam.faultline.survey.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "survey_active_landscape")
public class ActiveLandscapeEntity {

    public static final short SELECTION_SLOT = 1;

    @Id
    @Column(name = "selection_slot")
    private short selectionSlot;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    protected ActiveLandscapeEntity() {
    }

    public ActiveLandscapeEntity(UUID importId, Instant activatedAt) {
        this.selectionSlot = SELECTION_SLOT;
        this.importId = importId;
        this.activatedAt = activatedAt;
    }

    public UUID getImportId() {
        return importId;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }
}