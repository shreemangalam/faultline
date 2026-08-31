package dev.shreemangalam.faultline.restore.api;

/**
 * This verifies the independent Restore service boundary before recovery APIs are implemented.
 */
public record RestoreStatusResponse(String module, String status) {
}
