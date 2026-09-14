package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;
import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.persistence.CompletedRecoveryTiming;
import dev.shreemangalam.faultline.restore.persistence.RecoveryAttemptOutcomeCount;
import dev.shreemangalam.faultline.restore.persistence.RecoveryAttemptRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventStateCount;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecoveryStatisticsServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    private final RecoveryEventRepository eventRepository = mock(RecoveryEventRepository.class);
    private final RecoveryAttemptRepository attemptRepository = mock(RecoveryAttemptRepository.class);
    private final RecoveryStatisticsService service = new RecoveryStatisticsService(
            eventRepository,
            attemptRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void aggregatesAllStatesAndUsesTheNearestRankP95() {
        when(eventRepository.countByState()).thenReturn(List.of(
                new EventCount(RecoveryEventState.ELIGIBLE, 2),
                new EventCount(RecoveryEventState.BLOCKED, 1),
                new EventCount(RecoveryEventState.COMPLETED, 20),
                new EventCount(RecoveryEventState.FAILED_FINAL, 1)));
        when(attemptRepository.countByOutcome()).thenReturn(List.of(
                new AttemptCount(RecoveryAttemptOutcome.SUCCEEDED, 20),
                new AttemptCount(RecoveryAttemptOutcome.TRANSIENT_FAILURE, 2)));
        when(eventRepository.findCompletionTimings(RecoveryEventState.COMPLETED)).thenReturn(
                IntStream.rangeClosed(1, 20)
                        .mapToObj(seconds -> (CompletedRecoveryTiming) new CompletionTiming(NOW, NOW.plusSeconds(seconds)))
                        .toList());

        RecoveryStatistics statistics = service.get();

        assertThat(statistics.generatedAt()).isEqualTo(NOW);
        assertThat(statistics.totalEvents()).isEqualTo(24);
        assertThat(statistics.pendingEventCount()).isEqualTo(3);
        assertThat(statistics.terminalFailureCount()).isEqualTo(1);
        assertThat(statistics.eventCounts()).hasSize(RecoveryEventState.values().length);
        assertThat(statistics.eventCounts().get(RecoveryEventState.RECEIVED)).isZero();
        assertThat(statistics.totalAttempts()).isEqualTo(22);
        assertThat(statistics.attemptCounts()).hasSize(RecoveryAttemptOutcome.values().length);
        assertThat(statistics.attemptCounts().get(RecoveryAttemptOutcome.LEASE_EXPIRED)).isZero();
        assertThat(statistics.p95SuccessfulRecoveryLatencyMillis()).isEqualTo(19_000);
    }

    @Test
    void returnsZeroCountsAndNoLatencyWhenNoEventsExist() {
        when(eventRepository.countByState()).thenReturn(List.of());
        when(attemptRepository.countByOutcome()).thenReturn(List.of());
        when(eventRepository.findCompletionTimings(RecoveryEventState.COMPLETED)).thenReturn(List.of());

        RecoveryStatistics statistics = service.get();

        assertThat(statistics.totalEvents()).isZero();
        assertThat(statistics.pendingEventCount()).isZero();
        assertThat(statistics.terminalFailureCount()).isZero();
        assertThat(statistics.totalAttempts()).isZero();
        assertThat(statistics.p95SuccessfulRecoveryLatencyMillis()).isNull();
    }

    private record EventCount(RecoveryEventState state, long count) implements RecoveryEventStateCount {
        @Override
        public RecoveryEventState getState() { return state; }

        @Override
        public long getCount() { return count; }
    }

    private record AttemptCount(RecoveryAttemptOutcome outcome, long count) implements RecoveryAttemptOutcomeCount {
        @Override
        public RecoveryAttemptOutcome getOutcome() { return outcome; }

        @Override
        public long getCount() { return count; }
    }

    private record CompletionTiming(Instant createdAt, Instant completedAt) implements CompletedRecoveryTiming {
        @Override
        public Instant getCreatedAt() { return createdAt; }

        @Override
        public Instant getCompletedAt() { return completedAt; }
    }
}
