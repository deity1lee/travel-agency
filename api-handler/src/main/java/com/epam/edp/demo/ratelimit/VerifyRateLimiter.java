package com.epam.edp.demo.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window rate limiter for the OTP verify endpoints.
 *
 * <p>Caps how many verify requests a single key (source IP) may make per window, so an attacker
 * cannot parallelise guesses across many tokens or hammer one token faster than the per-token
 * attempt cap can react. This is the *online* brute-force control that complements the bcrypt
 * {@code tokenHash} (offline) and the per-token {@code attempts} counter.
 *
 * <p>State is per-instance and in memory: simple and dependency-free, sufficient for the
 * single-container deployment. A horizontally-scaled deployment would need a shared store
 * (Redis / a DynamoDB atomic counter) so the window is enforced across instances. The fixed
 * window also permits a 2x burst across a window boundary; a sliding window would tighten that.
 * Both are deliberately deferred — see issue 004.
 */
@Component
public class VerifyRateLimiter {

    private final Clock clock;
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public VerifyRateLimiter(Clock clock,
                             @Value("${app.ratelimit.verify.max-requests:10}") int maxRequests,
                             @Value("${app.ratelimit.verify.window-seconds:60}") long windowSeconds) {
        this.clock = clock;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    /**
     * Records a request against {@code key} and reports whether it is within the cap.
     *
     * @return {@code true} if the request is allowed, {@code false} if the key has exhausted its
     * window and the request should be rejected.
     */
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= windowMillis) {
                return new Window(now); // window expired (or first hit): start fresh at count 1
            }
            existing.count++;
            return existing;
        });
        return window.count <= maxRequests;
    }

    private static final class Window {
        private final long start;
        private int count;

        private Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}
