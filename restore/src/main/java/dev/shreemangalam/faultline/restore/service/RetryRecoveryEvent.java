package dev.shreemangalam.faultline.restore.service;
import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import java.time.Instant;
import java.util.UUID;
public record RetryRecoveryEvent(UUID eventId, RecoveryEventState state, Instant nextEligibleAt) { }