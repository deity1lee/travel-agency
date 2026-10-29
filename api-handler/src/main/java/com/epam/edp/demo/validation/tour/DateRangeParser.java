package com.epam.edp.demo.validation.tour;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

public final class DateRangeParser {

    private static final String SEPARATOR = " - ";
    private static final DateTimeFormatter SINGLE_DATE_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseSensitive()
            .appendPattern("MMM ")
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, java.time.format.SignStyle.NORMAL)
            .appendPattern(" yyyy")
            .toFormatter(Locale.ENGLISH);

    public enum Failure {
        UNPARSEABLE,
        FROM_AFTER_TO
    }

    @Getter
    @AllArgsConstructor
    public static final class DateRange {
        private final LocalDate from;
        private final LocalDate to;
    }

    @Getter
    public static final class Result {
        private final DateRange dateRange;
        private final Failure failure;

        private Result(DateRange dateRange, Failure failure) {
            this.dateRange = dateRange;
            this.failure = failure;
        }

        public static Result success(DateRange range) {
            return new Result(range, null);
        }

        public static Result failure(Failure failure) {
            return new Result(null, failure);
        }

        public boolean isSuccess() {
            return failure == null;
        }

    }

    private DateRangeParser() {
    }

    public static Result parse(String input) {
        if (input == null) {
            return Result.failure(Failure.UNPARSEABLE);
        }
        int separatorIndex = input.indexOf(SEPARATOR);
        if (separatorIndex < 0) {
            return Result.failure(Failure.UNPARSEABLE);
        }
        String fromPart = input.substring(0, separatorIndex);
        String toPart = input.substring(separatorIndex + SEPARATOR.length());
        if (fromPart.contains(SEPARATOR) || toPart.contains(SEPARATOR)) {
            return Result.failure(Failure.UNPARSEABLE);
        }
        LocalDate from = parseDate(fromPart);
        LocalDate to = parseDate(toPart);
        if (from == null || to == null) {
            return Result.failure(Failure.UNPARSEABLE);
        }
        if (from.isAfter(to)) {
            return Result.failure(Failure.FROM_AFTER_TO);
        }
        return Result.success(new DateRange(from, to));
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text, SINGLE_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
