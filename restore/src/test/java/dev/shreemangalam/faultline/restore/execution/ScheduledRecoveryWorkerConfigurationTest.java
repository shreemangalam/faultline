package dev.shreemangalam.faultline.restore.execution;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledRecoveryWorkerConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ScheduledRecoveryWorkerConfiguration.class)
            .withPropertyValues(
                    "restore.worker.enabled=true",
                    "restore.worker.id=configured-worker",
                    "restore.worker.batch-size=5",
                    "restore.worker.lease-seconds=60");

    @Test
    void rejectsAnInvalidBatchSize() {
        contextRunner
                .withPropertyValues("restore.worker.batch-size=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCauseMessage(context.getStartupFailure())).contains("batchSize", "greater than or equal to 1");
                });
    }

    @Test
    void rejectsAnUnsafeLeaseDuration() {
        contextRunner
                .withPropertyValues("restore.worker.lease-seconds=5")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCauseMessage(context.getStartupFailure())).contains("leaseSeconds", "greater than or equal to 30");
                });
    }

    private String rootCauseMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage();
    }
}
