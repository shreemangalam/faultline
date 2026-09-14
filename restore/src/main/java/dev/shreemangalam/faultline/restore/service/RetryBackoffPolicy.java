package dev.shreemangalam.faultline.restore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class RetryBackoffPolicy {
    private static final Duration BASE_DELAY = Duration.ofSeconds(5);
    private static final Duration MAX_DELAY = Duration.ofSeconds(60);

    private final RetryJitterSource jitterSource;
    private final Duration jitterWindow;

    RetryBackoffPolicy(
            RetryJitterSource jitterSource,
            @Value("${restore.retry.jitter-window:2s}") Duration jitterWindow) {
        if (jitterWindow.isNegative() || jitterWindow.compareTo(MAX_DELAY) > 0) {
            throw new IllegalArgumentException("Retry jitter window must be between 0 and 60 seconds.");
        }
        this.jitterSource = jitterSource;
        this.jitterWindow = jitterWindow;
    }

    Duration delayForAttempt(int attemptCount) {
        if (attemptCount < 1) throw new IllegalArgumentException("Attempt count must be positive.");
        long backoffSeconds = BASE_DELAY.toSeconds();
        for (int attempt = 1; attempt < attemptCount && backoffSeconds < MAX_DELAY.toSeconds(); attempt++) {
            backoffSeconds = Math.min(MAX_DELAY.toSeconds(), backoffSeconds * 2);
        }
        long jitterMillis = jitterWindow.isZero()
                ? 0
                : jitterSource.nextMillis(jitterWindow.toMillis() + 1);
        return Duration.ofSeconds(backoffSeconds).plusMillis(jitterMillis);
    }
}
