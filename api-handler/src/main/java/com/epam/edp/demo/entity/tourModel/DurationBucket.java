package com.epam.edp.demo.entity.tourModel;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum DurationBucket {
    DAYS_1_3("1-3 days", 1, 3),
    DAYS_4_7("4-7 days", 4, 7),
    DAYS_8_12("8-12 days", 8, 12),
    DAYS_13_PLUS("13+ days", 13, null);

    private final String wireValue;
    private final int minDays;
    private final Integer maxDays;

    DurationBucket(String wireValue, int minDays, Integer maxDays) {
        this.wireValue = wireValue;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public static Optional<DurationBucket> fromWireValue(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String normalized = input.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(b -> b.wireValue.equalsIgnoreCase(normalized))
                .findFirst();
    }
}
