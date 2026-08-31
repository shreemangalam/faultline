package dev.shreemangalam.faultline.survey.api;

/**
 * A deliberately small response contract used to verify the service boundary before domain APIs exist.
 */
public record SurveyStatusResponse(String module, String status) {
}
