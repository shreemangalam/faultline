package dev.shreemangalam.faultline.shared.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemIdTest {

    @Test
    void acceptsAStableUpperCaseIdentifier() {
        assertDoesNotThrow(() -> new SystemId("SAP_ECC"));
    }

    @Test
    void rejectsDisplayLabelsAndMalformedIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new SystemId("SAP ECC"));
    }
}
