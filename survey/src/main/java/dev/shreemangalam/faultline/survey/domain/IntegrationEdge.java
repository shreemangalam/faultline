package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

import java.util.Objects;

public record IntegrationEdge(
        String id,
        SystemId sourceSystemId,
        SystemId targetSystemId,
        String protocol,
        String dataDomain,
        SlaClass slaClass) {

    public IntegrationEdge {
        Objects.requireNonNull(id, "integration ID must not be null");
        Objects.requireNonNull(sourceSystemId, "integration source must not be null");
        Objects.requireNonNull(targetSystemId, "integration target must not be null");
        Objects.requireNonNull(protocol, "integration protocol must not be null");
        Objects.requireNonNull(dataDomain, "integration data domain must not be null");
        Objects.requireNonNull(slaClass, "integration SLA class must not be null");
        if (!id.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException("integration ID must use upper-case letters, digits, and underscores");
        }
        protocol = protocol.trim();
        dataDomain = dataDomain.trim();
        if (protocol.isEmpty()) {
            throw new IllegalArgumentException("integration protocol must not be blank");
        }
        if (dataDomain.isEmpty()) {
            throw new IllegalArgumentException("integration data domain must not be blank");
        }
    }

    public IntegrationEdge(String id, SystemId sourceSystemId, SystemId targetSystemId, String protocol) {
        this(id, sourceSystemId, targetSystemId, protocol, "GENERAL", SlaClass.BATCH);
    }
}