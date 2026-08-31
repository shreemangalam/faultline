package dev.shreemangalam.faultline.survey.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ImpactAnalysisRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String systemId,
        @NotNull DegradationType degradationType) {
}
