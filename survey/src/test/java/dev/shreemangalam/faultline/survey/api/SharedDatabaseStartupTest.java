package dev.shreemangalam.faultline.survey.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        // Load deployment settings rather than the test-only application.yaml.
        "spring.config.location=file:src/main/resources/application.yaml",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///faultline-survey-restore-first?TC_INITSCRIPT=restore-first-startup.sql"
})
class SharedDatabaseStartupTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void initializesAllSurveyMigrationsWithoutChangingExistingSharedTables() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM restore_startup_evidence WHERE id=1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success AND type='SQL'", Integer.class))
                .isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM survey_landscape_import", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM survey_impact_report", Integer.class)).isZero();
    }
}
