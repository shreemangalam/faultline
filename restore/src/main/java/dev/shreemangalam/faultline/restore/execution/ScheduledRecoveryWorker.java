package dev.shreemangalam.faultline.restore.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix = "restore.worker", name = "enabled", havingValue = "true")
public class ScheduledRecoveryWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledRecoveryWorker.class);

    private final RecoveryBatchExecutor executor;
    private final ScheduledRecoveryWorkerProperties properties;
    private final AtomicBoolean running = new AtomicBoolean();

    public ScheduledRecoveryWorker(
            RecoveryBatchExecutor executor,
            ScheduledRecoveryWorkerProperties properties) {
        this.executor = executor;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${restore.worker.fixed-delay:1s}",
            initialDelayString = "${restore.worker.initial-delay:1s}")
    void runOnce() {
        if (!running.compareAndSet(false, true)) return;
        try {
            var executed = executor.execute(new ExecuteRecoveryBatchCommand(
                    properties.id(), properties.batchSize(), properties.leaseSeconds()));
            if (!executed.isEmpty()) {
                LOGGER.info("Worker {} executed {} recovery event(s).", properties.id(), executed.size());
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Worker {} batch failed; the next scheduled run will retry.", properties.id(), exception);
        } finally {
            running.set(false);
        }
    }
}
