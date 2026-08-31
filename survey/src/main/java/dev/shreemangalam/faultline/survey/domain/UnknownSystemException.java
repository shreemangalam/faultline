package dev.shreemangalam.faultline.survey.domain;

import dev.shreemangalam.faultline.shared.domain.SystemId;

public final class UnknownSystemException extends IllegalArgumentException {

    public UnknownSystemException(SystemId systemId) {
        super("unknown system ID: " + systemId.value());
    }
}
