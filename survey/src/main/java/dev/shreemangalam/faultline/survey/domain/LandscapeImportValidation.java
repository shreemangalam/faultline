package dev.shreemangalam.faultline.survey.domain;

import java.util.List;

public record LandscapeImportValidation(
        boolean valid,
        int systemCount,
        int integrationCount,
        List<ValidationIssue> issues) {

    public LandscapeImportValidation {
        issues = List.copyOf(issues);
    }

    public record ValidationIssue(String field, String message) {
    }
}
