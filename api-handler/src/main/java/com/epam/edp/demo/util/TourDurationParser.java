package com.epam.edp.demo.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TourDurationParser {

    private static final Pattern DAYS_PATTERN = Pattern.compile("^(\\d+)\\s+days?$", Pattern.CASE_INSENSITIVE);

    private TourDurationParser() {
    }

    public static Optional<Integer> parseDays(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = DAYS_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}