package dev.shreemangalam.faultline.restore.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
class ThreadLocalRetryJitterSource implements RetryJitterSource {
    @Override
    public long nextMillis(long upperBoundExclusive) {
        return ThreadLocalRandom.current().nextLong(upperBoundExclusive);
    }
}
