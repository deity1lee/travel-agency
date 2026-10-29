package com.epam.edp.demo.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerifyRateLimiterTest {

    private final Clock clock = mock(Clock.class);
    // 3 requests per 60s window keeps the arithmetic obvious in the assertions.
    private final VerifyRateLimiter limiter = new VerifyRateLimiter(clock, 3, 60);

    @Test
    void tryAcquire_allowsUpToLimitThenRejectsWithinWindow() {
        when(clock.millis()).thenReturn(1_000L);

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();  // 1
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();  // 2
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();  // 3
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse(); // 4th over the cap
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse(); // still rejected
    }

    @Test
    void tryAcquire_resetsCounterOncePastTheWindow() {
        when(clock.millis()).thenReturn(1_000L);
        limiter.tryAcquire("1.2.3.4");
        limiter.tryAcquire("1.2.3.4");
        limiter.tryAcquire("1.2.3.4");
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse(); // capped within the first window

        // Advance past the 60s window — the key gets a fresh allowance.
        when(clock.millis()).thenReturn(1_000L + 60_000L);
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
    }

    @Test
    void tryAcquire_tracksKeysIndependently() {
        when(clock.millis()).thenReturn(1_000L);
        limiter.tryAcquire("1.1.1.1");
        limiter.tryAcquire("1.1.1.1");
        limiter.tryAcquire("1.1.1.1");
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse(); // first key exhausted

        // A different IP is unaffected by the first key's count.
        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();
    }
}
