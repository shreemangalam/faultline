package dev.shreemangalam.faultline.shared.domain;

import java.util.Objects;

/**
 * A stable, human-readable system identifier shared across API contracts and persisted relationships.
 * Keeping this as a value object prevents accidental use of display labels as identifiers.
 */
public record SystemId(String value) {

    public SystemId {
        Objects.requireNonNull(value, "system ID must not be null");
        if (!value.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException(
                    "system ID must use upper-case letters, digits, and underscores and be 2-64 characters long");
        }
    }
}
