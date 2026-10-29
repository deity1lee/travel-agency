package com.epam.edp.demo.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class JitteredBackoff {

    private static final long MIN_MS = 30;
    private static final long MAX_MS = 80;

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis);
    }

    private final Sleeper sleeper;

    public JitteredBackoff() {
        this(JitteredBackoff::defaultSleep);
    }

    public JitteredBackoff(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    public void sleep() {
        sleeper.sleep(ThreadLocalRandom.current().nextLong(MIN_MS, MAX_MS));
    }

    private static void defaultSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
