package dev.shreemangalam.faultline.restore.simulator;

public enum DownstreamBehavior {
    SUCCESS,
    TEMPORARY_UNAVAILABLE,
    RATE_LIMITED,
    DELAYED_SUCCESS,
    VALIDATION_FAILURE
}
