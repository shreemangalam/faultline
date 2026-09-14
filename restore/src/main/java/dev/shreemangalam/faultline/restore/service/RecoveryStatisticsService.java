package dev.shreemangalam.faultline.restore.service;

import dev.shreemangalam.faultline.restore.domain.RecoveryAttemptOutcome;
import dev.shreemangalam.faultline.restore.domain.RecoveryEventState;
import dev.shreemangalam.faultline.restore.persistence.CompletedRecoveryTiming;
import dev.shreemangalam.faultline.restore.persistence.RecoveryAttemptRepository;
import dev.shreemangalam.faultline.restore.persistence.RecoveryEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RecoveryStatisticsService {
    private static final List<RecoveryEventState> PENDING_STATES = List.of(
            RecoveryEventState.RECEIVED,
            RecoveryEventState.ELIGIBLE,
            RecoveryEventState.BLOCKED,
            RecoveryEventState.IN_PROGRESS,
            RecoveryEventState.RETRY_SCHEDULED);
    private static final List<RecoveryEventState> TERMINAL_FAILURE_STATES = List.of(
            RecoveryEventState.EXHAUSTED,
            RecoveryEventState.FAILED_FINAL,
            RecoveryEventState.FAILED_DEPENDENCY);

    private final RecoveryEventRepository eventRepository;
    private final RecoveryAttemptRepository attemptRepository;
    private final Clock clock;

    public RecoveryStatisticsService(
            RecoveryEventRepository eventRepository,
            RecoveryAttemptRepository attemptRepository,
            Clock clock) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RecoveryStatistics get() {
        EnumMap<RecoveryEventState, Long> eventCounts = zeroEventCounts();
        eventRepository.countByState().forEach(count -> eventCounts.put(count.getState(), count.getCount()));

        EnumMap<RecoveryAttemptOutcome, Long> attemptCounts = zeroAttemptCounts();
        attemptRepository.countByOutcome().forEach(count -> attemptCounts.put(count.getOutcome(), count.getCount()));

        return new RecoveryStatistics(
                clock.instant(),
                sum(eventCounts),
                sumStates(eventCounts, PENDING_STATES),
                sumStates(eventCounts, TERMINAL_FAILURE_STATES),
                immutable(eventCounts),
                sum(attemptCounts),
                immutable(attemptCounts),
                nearestRankP95Millis(eventRepository.findCompletionTimings(RecoveryEventState.COMPLETED)));
    }

    static Long nearestRankP95Millis(List<CompletedRecoveryTiming> timings) {
        if (timings.isEmpty()) return null;

        List<Long> latencies = timings.stream()
                .map(timing -> Duration.between(timing.getCreatedAt(), timing.getCompletedAt()).toMillis())
                .sorted()
                .toList();
        int rank = (int) Math.ceil(0.95 * latencies.size());
        return latencies.get(rank - 1);
    }

    private static EnumMap<RecoveryEventState, Long> zeroEventCounts() {
        EnumMap<RecoveryEventState, Long> counts = new EnumMap<>(RecoveryEventState.class);
        for (RecoveryEventState state : RecoveryEventState.values()) counts.put(state, 0L);
        return counts;
    }

    private static EnumMap<RecoveryAttemptOutcome, Long> zeroAttemptCounts() {
        EnumMap<RecoveryAttemptOutcome, Long> counts = new EnumMap<>(RecoveryAttemptOutcome.class);
        for (RecoveryAttemptOutcome outcome : RecoveryAttemptOutcome.values()) counts.put(outcome, 0L);
        return counts;
    }

    private static <E extends Enum<E>> long sum(Map<E, Long> counts) {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }

    private static long sumStates(Map<RecoveryEventState, Long> counts, List<RecoveryEventState> states) {
        return states.stream().mapToLong(counts::get).sum();
    }

    private static <E extends Enum<E>> Map<E, Long> immutable(EnumMap<E, Long> counts) {
        return Collections.unmodifiableMap(new EnumMap<>(counts));
    }
}
