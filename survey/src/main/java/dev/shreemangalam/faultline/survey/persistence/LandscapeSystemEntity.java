package dev.shreemangalam.faultline.survey.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "survey_landscape_system")
public class LandscapeSystemEntity {

    @Id
    private UUID id;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "system_id", nullable = false)
    private String systemId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int criticality;

    protected LandscapeSystemEntity() {
    }

    public LandscapeSystemEntity(UUID id, UUID importId, String systemId, String label, int criticality) {
        this.id = id;
        this.importId = importId;
        this.systemId = systemId;
        this.label = label;
        this.criticality = criticality;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getLabel() {
        return label;
    }

    public int getCriticality() {
        return criticality;
    }
}
