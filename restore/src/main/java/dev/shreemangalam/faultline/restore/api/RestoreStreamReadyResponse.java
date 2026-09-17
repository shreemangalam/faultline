package dev.shreemangalam.faultline.restore.api;

import java.time.Instant;

public record RestoreStreamReadyResponse(String status, Instant connectedAt) {
}
