package dev.shreemangalam.faultline.survey.domain;

public enum SlaClass {
    REAL_TIME(20),
    NEAR_REAL_TIME(10),
    BATCH(0);

    private final int impactPoints;

    SlaClass(int impactPoints) {
        this.impactPoints = impactPoints;
    }

    public int impactPoints() {
        return impactPoints;
    }
}
