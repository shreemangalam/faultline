package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.service.StoredImpactReport;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoredImpactReportResponse(
        UUID reportId,
        Instant createdAt,
        String landscapeReference,
        StoredImpactReport.Incident incident,
        List<StoredImpactReport.AffectedSystem> affectedSystems,
        List<StoredImpactReport.AffectedIntegration> affectedIntegrations,
        List<StoredImpactReport.Priority> priorities,
        StoredImpactReport.Recovery recovery,
        List<String> warnings) {
    static StoredImpactReportResponse from(StoredImpactReport report) {
        var snapshot = report.snapshot();
        return new StoredImpactReportResponse(report.reportId(), report.createdAt(), report.landscapeReference(), snapshot.incident(), snapshot.affectedSystems(), snapshot.affectedIntegrations(), snapshot.priorities(), snapshot.recovery(), snapshot.warnings());
    }
}