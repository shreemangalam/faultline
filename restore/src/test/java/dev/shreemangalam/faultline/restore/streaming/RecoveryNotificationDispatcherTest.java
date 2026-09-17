package dev.shreemangalam.faultline.restore.streaming;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RecoveryNotificationDispatcherTest {
    @Test
    void keepsPublishersNonBlockingAndDiscardsTheOldestBufferedNotification() throws Exception {
        RecoveryNotificationDispatcher dispatcher = new RecoveryNotificationDispatcher(2);
        CountDownLatch deliveryStarted = new CountDownLatch(1);
        CountDownLatch releaseDelivery = new CountDownLatch(1);
        CountDownLatch retainedNotificationsDelivered = new CountDownLatch(2);
        List<Integer> delivered = new CopyOnWriteArrayList<>();

        try {
            dispatcher.dispatch(() -> {
                delivered.add(1);
                deliveryStarted.countDown();
                await(releaseDelivery);
            });
            assertThat(deliveryStarted.await(1, TimeUnit.SECONDS)).isTrue();

            dispatcher.dispatch(() -> delivered.add(2));
            dispatcher.dispatch(() -> {
                delivered.add(3);
                retainedNotificationsDelivered.countDown();
            });

            assertThatCode(() -> CompletableFuture.runAsync(() -> dispatcher.dispatch(() -> {
                        delivered.add(4);
                        retainedNotificationsDelivered.countDown();
                    }))
                    .get(1, TimeUnit.SECONDS))
                    .doesNotThrowAnyException();

            releaseDelivery.countDown();
            assertThat(retainedNotificationsDelivered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(delivered).containsExactly(1, 3, 4);
        } finally {
            releaseDelivery.countDown();
            dispatcher.stop();
        }
    }

    @Test
    void rejectsANonPositiveBufferCapacity() {
        assertThatCode(() -> new RecoveryNotificationDispatcher(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stream notification buffer capacity must be positive.");
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the test delivery to resume.", exception);
        }
    }
}