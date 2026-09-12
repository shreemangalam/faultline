package dev.shreemangalam.faultline.restore.execution;

public record ExecuteRecoveryBatchCommand(String workerId, int maxEvents, int leaseSeconds) {
}
