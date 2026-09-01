package dev.shreemangalam.faultline.restore.api;

import dev.shreemangalam.faultline.restore.domain.CircuitState;

import java.time.Instant;

public record CircuitBreakerResponse(String targetSystem, CircuitState state, int consecutiveFailures, Instant openUntil, Instant updatedAt) { }