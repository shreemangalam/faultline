package dev.shreemangalam.faultline.survey.domain;

public enum SeverityBand {
    CRITICAL,
    HIGH,
    MODERATE,
    LOW;

    public static SeverityBand fromScore(int score) {
        if (score >= 120) {
            return CRITICAL;
        }
        if (score >= 90) {
            return HIGH;
        }
        if (score >= 60) {
            return MODERATE;
        }
        return LOW;
    }
}
