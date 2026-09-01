package dev.shreemangalam.faultline.survey.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "survey_impact_report")
public class ImpactReportEntity {
    @Id private UUID id;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "failed_system_id", nullable = false) private String failedSystemId;
    @Column(name = "degradation_type", nullable = false) private String degradationType;
    @Column(name = "landscape_reference", nullable = false) private String landscapeReference;
    @Column(name = "report_json", nullable = false) private String reportJson;
    protected ImpactReportEntity() { }
    public ImpactReportEntity(UUID id, Instant createdAt, String failedSystemId, String degradationType, String landscapeReference, String reportJson) {
        this.id = id; this.createdAt = createdAt; this.failedSystemId = failedSystemId; this.degradationType = degradationType; this.landscapeReference = landscapeReference; this.reportJson = reportJson;
    }
    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getLandscapeReference() { return landscapeReference; }
    public String getReportJson() { return reportJson; }
}