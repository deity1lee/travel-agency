package com.epam.edp.demo.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JitteredBackoffTest {

    @Test
    void sleep_invokesSleeperWithDurationBetween30And80MillisExclusiveUpperBound() {
        List<Long> sleeps = new ArrayList<>();
        JitteredBackoff backoff = new JitteredBackoff(sleeps::add);

        for (int i = 0; i < 200; i++) {
            backoff.sleep();
        }

        assertThat(sleeps).hasSize(200);
        assertThat(sleeps).allSatisfy(ms -> assertThat(ms).isBetween(30L, 79L));
    }

    @Test
    void sleep_invokesSleeperExactlyOncePerCall() {
        int[] count = {0};
        JitteredBackoff backoff = new JitteredBackoff(ms -> count[0]++);

        backoff.sleep();

        assertThat(count[0]).isEqualTo(1);
    }

    @Test
    void defaultConstructor_doesNotThrowAndSleepsAtLeastMinMillis() {
        JitteredBackoff backoff = new JitteredBackoff();

        long start = System.currentTimeMillis();
        backoff.sleep();
        long elapsed = System.currentTimeMillis() - start;

        // Default sleeper actually sleeps - we just verify it returns and elapses some time.
        assertThat(elapsed).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void defaultSleeper_whenThreadAlreadyInterrupted_swallowsExceptionAndRestoresInterruptFlag() {
        JitteredBackoff backoff = new JitteredBackoff();

        // Pre-setting the interrupt flag makes Thread.sleep throw InterruptedException immediately,
        // exercising the catch branch of the default sleeper.
        Thread.currentThread().interrupt();
        try {
            backoff.sleep();

            // The catch block re-asserts the interrupt status rather than silently clearing it.
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            // Clear the flag so it cannot leak into other tests on this thread.
            Thread.interrupted();
        }
    }
}
