package dev.shreemangalam.faultline.shared.domain;

/**
 * Operational health is kept separate from landscape configuration because it changes during incidents.
 */
public enum HealthStatus {
    HEALTHY,
    DEGRADED,
    UNAVAILABLE,
    UNKNOWN
}
