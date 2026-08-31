package dev.shreemangalam.faultline.survey.api;

import dev.shreemangalam.faultline.survey.domain.LandscapeImportValidation;

import java.util.List;

public record LandscapeImportValidationResponse(
        boolean valid,
        int systemCount,
        int integrationCount,
        List<ValidationIssueResponse> issues) {

    static LandscapeImportValidationResponse from(LandscapeImportValidation validation) {
        return new LandscapeImportValidationResponse(
                validation.valid(),
                validation.systemCount(),
                validation.integrationCount(),
                validation.issues().stream()
                        .map(issue -> new ValidationIssueResponse(issue.field(), issue.message()))
                        .toList());
    }

    public record ValidationIssueResponse(String field, String message) {
    }
}
