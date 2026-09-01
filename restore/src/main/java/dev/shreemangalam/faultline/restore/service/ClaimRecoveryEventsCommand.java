package dev.shreemangalam.faultline.restore.service;

public record ClaimRecoveryEventsCommand(String workerId, int maxEvents, int leaseSeconds) {
}