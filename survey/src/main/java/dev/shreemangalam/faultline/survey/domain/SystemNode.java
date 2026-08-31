package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.Objects;

public record SystemNode(SystemId id, String label, int criticality) {

    public SystemNode {
        Objects.requireNonNull(id, "system ID must not be null");
        Objects.requireNonNull(label, "system label must not be null");
        label = label.trim();
        if (label.isEmpty()) {
            throw new IllegalArgumentException("system label must not be blank");
        }
        if (criticality < 1 || criticality > 10) {
            throw new IllegalArgumentException("criticality must be between 1 and 10");
        }
    }
}
