package dev.shreemangalam.faultline.restore.service;

@FunctionalInterface
interface RetryJitterSource {
    long nextMillis(long upperBoundExclusive);
}
