package dev.shreemangalam.faultline.restore.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledRecoveryWorkerTest {
    @Mock
    private RecoveryBatchExecutor executor;

    @Test
    void executesABatchWithConfiguredWorkerSettings() {
        var worker = worker();
        when(executor.execute(any())).thenReturn(List.of());

        worker.runOnce();

        ArgumentCaptor<ExecuteRecoveryBatchCommand> command = ArgumentCaptor.forClass(ExecuteRecoveryBatchCommand.class);
        verify(executor).execute(command.capture());
        assertThat(command.getValue()).isEqualTo(new ExecuteRecoveryBatchCommand("scheduled-worker", 4, 45));
    }

    @Test
    void skipsAnOverlappingTick() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(executor.execute(any())).thenAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(2, TimeUnit.SECONDS)).isTrue();
            return List.of();
        });
        var worker = worker();
        Thread firstRun = Thread.ofPlatform().start(worker::runOnce);

        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        worker.runOnce();
        verify(executor, times(1)).execute(any());

        release.countDown();
        firstRun.join(2_000);
        assertThat(firstRun.isAlive()).isFalse();
    }

    @Test
    void releasesTheGuardAfterABatchFailure() {
        when(executor.execute(any()))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(List.of());
        var worker = worker();

        assertThatCode(worker::runOnce).doesNotThrowAnyException();
        worker.runOnce();

        verify(executor, times(2)).execute(any());
    }

    private ScheduledRecoveryWorker worker() {
        return new ScheduledRecoveryWorker(
                executor,
                new ScheduledRecoveryWorkerProperties("scheduled-worker", 4, 45));
    }
}
