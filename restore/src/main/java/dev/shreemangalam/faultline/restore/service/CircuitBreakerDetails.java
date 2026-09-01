package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.CircuitState;

import java.time.Instant;

public record CircuitBreakerDetails(String targetSystem, CircuitState state, int consecutiveFailures, Instant openUntil, Instant updatedAt) { }