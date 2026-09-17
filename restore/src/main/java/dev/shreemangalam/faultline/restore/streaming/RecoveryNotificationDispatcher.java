package dev.shreemangalam.faultline.restore.streaming;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class RecoveryNotificationDispatcher {
    private final ThreadPoolExecutor executor;

    public RecoveryNotificationDispatcher(
            @Value("${restore.stream.notification-buffer-capacity:256}") int bufferCapacity) {
        if (bufferCapacity <= 0) {
            throw new IllegalArgumentException("Stream notification buffer capacity must be positive.");
        }
        executor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(bufferCapacity),
                Thread.ofPlatform().daemon().name("restore-sse-notification").factory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void dispatch(Runnable notification) {
        executor.execute(notification);
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}