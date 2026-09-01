package dev.shreemangalam.faultline.survey.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoredImpactReport(UUID reportId, Instant createdAt, String landscapeReference, Snapshot snapshot) {
    public record Snapshot(
            Incident incident,
            List<AffectedSystem> affectedSystems,
            List<AffectedIntegration> affectedIntegrations,
            List<Priority> priorities,
            Recovery recovery,
            List<String> warnings) {
        public Snapshot {
            affectedSystems = List.copyOf(affectedSystems);
            affectedIntegrations = List.copyOf(affectedIntegrations);
            priorities = List.copyOf(priorities);
            warnings = List.copyOf(warnings);
        }
    }
    public record Incident(String systemId, String degradationType) { }
    public record AffectedSystem(String systemId, String label, int impactRing, String classification) { }
    public record AffectedIntegration(String integrationId, String sourceSystemId, String targetSystemId, String protocol, String dataDomain, String slaClass, int impactRing) { }
    public record Priority(String systemId, String label, int impactRing, int score, String severity, ScoreBreakdown scoreBreakdown) { }
    public record ScoreBreakdown(int criticalityPoints, int fanOutPoints, int slaPoints, int proximityPoints) { }
    public record Recovery(List<String> recoverySequence, boolean cycleWarning, List<String> unresolvedSystemIds) {
        public Recovery { recoverySequence = List.copyOf(recoverySequence); unresolvedSystemIds = List.copyOf(unresolvedSystemIds); }
    }
}