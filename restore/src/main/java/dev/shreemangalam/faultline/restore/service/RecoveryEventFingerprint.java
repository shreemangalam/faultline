package dev.shreemangalam.faultline.restore.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RecoveryEventFingerprint {
    private RecoveryEventFingerprint() {
    }

    public static String calculate(CreateRecoveryEventCommand command) {
        String prerequisiteIds = command.prerequisiteEventIds().stream()
                .map(UUID::toString)
                .sorted()
                .collect(Collectors.joining(","));
        String canonical = String.join("\u001F", command.correlationId(), command.targetSystem(), command.payloadReference(),
                Integer.toString(command.priority()), command.deadlineAt() == null ? "" : command.deadlineAt().toString(), prerequisiteIds);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the JVM.", exception);
        }
    }
}
