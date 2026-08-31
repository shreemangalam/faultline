package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LandscapeImportValidator {

    public LandscapeImportValidation validate(ImportDefinition definition) {
        List<LandscapeImportValidation.ValidationIssue> issues = new ArrayList<>();
        List<ImportSystem> systems = definition.systems() == null ? List.of() : definition.systems();
        List<ImportIntegration> integrations = definition.integrations() == null ? List.of() : definition.integrations();

        if (systems.isEmpty()) { issues.add(issue("systems", "at least one system is required")); }
        if (integrations.isEmpty()) { issues.add(issue("integrations", "at least one integration is required")); }

        Set<String> systemIds = new HashSet<>();
        for (int index = 0; index < systems.size(); index++) {
            ImportSystem system = systems.get(index);
            String path = "systems[" + index + "]";
            if (system == null) { issues.add(issue(path, "system must not be null")); continue; }
            validateSystemId(system.id(), path + ".id", issues);
            if (system.id() != null && !systemIds.add(system.id())) { issues.add(issue(path + ".id", "system ID must be unique")); }
            if (system.label() == null || system.label().isBlank()) { issues.add(issue(path + ".label", "system label must not be blank")); }
            if (system.criticality() < 1 || system.criticality() > 10) { issues.add(issue(path + ".criticality", "criticality must be between 1 and 10")); }
        }

        Set<String> integrationIds = new HashSet<>();
        for (int index = 0; index < integrations.size(); index++) {
            ImportIntegration integration = integrations.get(index);
            String path = "integrations[" + index + "]";
            if (integration == null) { issues.add(issue(path, "integration must not be null")); continue; }
            validateIntegrationId(integration.id(), path + ".id", issues);
            if (integration.id() != null && !integrationIds.add(integration.id())) { issues.add(issue(path + ".id", "integration ID must be unique")); }
            validateSystemId(integration.sourceSystemId(), path + ".sourceSystemId", issues);
            validateSystemId(integration.targetSystemId(), path + ".targetSystemId", issues);
            if (integration.sourceSystemId() != null && !systemIds.contains(integration.sourceSystemId())) { issues.add(issue(path + ".sourceSystemId", "source system must exist in the import")); }
            if (integration.targetSystemId() != null && !systemIds.contains(integration.targetSystemId())) { issues.add(issue(path + ".targetSystemId", "target system must exist in the import")); }
            if (integration.protocol() == null || integration.protocol().isBlank()) { issues.add(issue(path + ".protocol", "protocol must not be blank")); }
            if (integration.dataDomain() == null || integration.dataDomain().isBlank()) { issues.add(issue(path + ".dataDomain", "data domain must not be blank")); }
            try { SlaClass.fromValue(integration.slaClass()); } catch (RuntimeException exception) { issues.add(issue(path + ".slaClass", "SLA class must be REAL_TIME, NEAR_REAL_TIME, or BATCH")); }
        }
        return new LandscapeImportValidation(issues.isEmpty(), systems.size(), integrations.size(), issues);
    }

    private void validateSystemId(String value, String field, List<LandscapeImportValidation.ValidationIssue> issues) {
        if (value == null) { issues.add(issue(field, "system ID must not be null")); return; }
        try { new SystemId(value); } catch (IllegalArgumentException exception) { issues.add(issue(field, exception.getMessage())); }
    }

    private void validateIntegrationId(String value, String field, List<LandscapeImportValidation.ValidationIssue> issues) {
        if (value == null || !value.matches("[A-Z][A-Z0-9_]{1,63}")) { issues.add(issue(field, "integration ID must use upper-case letters, digits, and underscores")); }
    }

    private LandscapeImportValidation.ValidationIssue issue(String field, String message) { return new LandscapeImportValidation.ValidationIssue(field, message); }

    public record ImportDefinition(List<ImportSystem> systems, List<ImportIntegration> integrations) {
    }
    public record ImportSystem(String id, String label, int criticality) {
    }
    public record ImportIntegration(String id, String sourceSystemId, String targetSystemId, String protocol, String dataDomain, String slaClass) {
        public ImportIntegration {
            dataDomain = dataDomain == null ? "GENERAL" : dataDomain.trim();
            slaClass = slaClass == null ? "BATCH" : slaClass.trim().toUpperCase(Locale.ROOT);
        }

        public ImportIntegration(String id, String sourceSystemId, String targetSystemId, String protocol) {
            this(id, sourceSystemId, targetSystemId, protocol, "GENERAL", "BATCH");
        }
    }
}