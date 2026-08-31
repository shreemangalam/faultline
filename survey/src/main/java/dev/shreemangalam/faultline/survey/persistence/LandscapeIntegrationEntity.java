package dev.shreemangalam.faultline.survey.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "survey_landscape_integration")
public class LandscapeIntegrationEntity {

    @Id
    private UUID id;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "integration_id", nullable = false)
    private String integrationId;

    @Column(name = "source_system_id", nullable = false)
    private String sourceSystemId;

    @Column(name = "target_system_id", nullable = false)
    private String targetSystemId;

    @Column(nullable = false)
    private String protocol;

    protected LandscapeIntegrationEntity() {
    }

    public LandscapeIntegrationEntity(
            UUID id,
            UUID importId,
            String integrationId,
            String sourceSystemId,
            String targetSystemId,
            String protocol) {
        this.id = id;
        this.importId = importId;
        this.integrationId = integrationId;
        this.sourceSystemId = sourceSystemId;
        this.targetSystemId = targetSystemId;
        this.protocol = protocol;
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public String getSourceSystemId() {
        return sourceSystemId;
    }

    public String getTargetSystemId() {
        return targetSystemId;
    }

    public String getProtocol() {
        return protocol;
    }
}
