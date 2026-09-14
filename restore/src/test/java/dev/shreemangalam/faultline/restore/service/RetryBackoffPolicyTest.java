package dev.shreemangalam.faultline.restore.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RetryBackoffPolicyTest {
    @Test
    void addsJitterWithinTheConfiguredInclusiveWindow() {
        AtomicLong requestedBound = new AtomicLong();
        RetryJitterSource maximumJitter = upperBoundExclusive -> {
            requestedBound.set(upperBoundExclusive);
            return upperBoundExclusive - 1;
        };
        var policy = new RetryBackoffPolicy(maximumJitter, Duration.ofSeconds(2));

        assertThat(policy.delayForAttempt(1)).isEqualTo(Duration.ofSeconds(7));
        assertThat(policy.delayForAttempt(2)).isEqualTo(Duration.ofSeconds(12));
        assertThat(policy.delayForAttempt(3)).isEqualTo(Duration.ofSeconds(22));
        assertThat(requestedBound).hasValue(2_001);
    }

    @Test
    void capsBackoffBeforeAddingJitter() {
        var policy = new RetryBackoffPolicy(upperBoundExclusive -> 500, Duration.ofSeconds(2));

        assertThat(policy.delayForAttempt(10)).isEqualTo(Duration.ofMillis(60_500));
    }

    @Test
    void supportsZeroJitterWithoutCallingTheRandomSource() {
        var policy = new RetryBackoffPolicy(
                upperBoundExclusive -> { throw new AssertionError("Random source should not be called."); },
                Duration.ZERO);

        assertThat(policy.delayForAttempt(2)).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void rejectsInvalidPolicyInput() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RetryBackoffPolicy(bound -> 0, Duration.ofSeconds(-1)))
                .withMessageContaining("between 0 and 60 seconds");
        var policy = new RetryBackoffPolicy(bound -> 0, Duration.ofSeconds(2));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.delayForAttempt(0))
                .withMessageContaining("positive");
    }
}
