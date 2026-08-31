package dev.shreemangalam.faultline.survey.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LandscapeImportValidatorTest {

    private final LandscapeImportValidator validator = new LandscapeImportValidator();

    @Test
    void acceptsAConsistentLandscapeDefinition() {
        LandscapeImportValidation validation = validator.validate(new LandscapeImportValidator.ImportDefinition(
                List.of(
                        new LandscapeImportValidator.ImportSystem("SAP_ECC", "SAP ECC", 10),
                        new LandscapeImportValidator.ImportSystem("CPI_ORDER_SYNC", "CPI Order Sync", 9)),
                List.of(new LandscapeImportValidator.ImportIntegration(
                        "ECC_TO_ORDER", "SAP_ECC", "CPI_ORDER_SYNC", "RFC"))));

        assertThat(validation.valid()).isTrue();
        assertThat(validation.systemCount()).isEqualTo(2);
        assertThat(validation.integrationCount()).isEqualTo(1);
        assertThat(validation.issues()).isEmpty();
    }

    @Test
    void reportsMultipleInvalidFieldsInOnePass() {
        LandscapeImportValidation validation = validator.validate(new LandscapeImportValidator.ImportDefinition(
                List.of(
                        new LandscapeImportValidator.ImportSystem("SAP_ECC", "", 11),
                        new LandscapeImportValidator.ImportSystem("SAP_ECC", "Duplicate", 5)),
                List.of(
                        new LandscapeImportValidator.ImportIntegration("bad edge", "SAP_ECC", "MISSING", ""),
                        new LandscapeImportValidator.ImportIntegration("bad edge", "SAP_ECC", "MISSING", "REST"))));

        assertThat(validation.valid()).isFalse();
        assertThat(validation.issues()).extracting(LandscapeImportValidation.ValidationIssue::field)
                .contains("systems[0].label", "systems[0].criticality", "systems[1].id",
                        "integrations[0].id", "integrations[0].targetSystemId", "integrations[0].protocol",
                        "integrations[1].id");
    }
}
