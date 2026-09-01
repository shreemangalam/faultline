package dev.shreemangalam.faultline.restore.service;
import java.util.UUID;
public record RetryRecoveryEventCommand(UUID eventId, String workerId, String reason) { }