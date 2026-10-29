package com.epam.edp.demo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TourDurationParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "days", "7", "7d", "seven days", "7 day trip", "abc days"})
    void parseDays_returnsEmpty_forInvalidInput(String input) {
        assertThat(TourDurationParser.parseDays(input)).isEmpty();
    }

    @Test
    void parseDays_acceptsSingularDay() {
        assertThat(TourDurationParser.parseDays("1 day")).contains(1);
    }

    @Test
    void parseDays_acceptsPluralDays() {
        assertThat(TourDurationParser.parseDays("7 days")).contains(7);
    }

    @Test
    void parseDays_isCaseInsensitive() {
        assertThat(TourDurationParser.parseDays("14 DAYS")).contains(14);
        assertThat(TourDurationParser.parseDays("14 Days")).contains(14);
    }

    @Test
    void parseDays_trimsWhitespace() {
        assertThat(TourDurationParser.parseDays("  10 days  ")).contains(10);
    }

    @Test
    void parseDays_acceptsMultiDigit() {
        Optional<Integer> result = TourDurationParser.parseDays("365 days");
        assertThat(result).contains(365);
    }

    @Test
    void parseDays_returnsEmpty_whenNumberOverflowsInt() {
        // Matches the digit pattern but exceeds Integer.MAX_VALUE, tripping NumberFormatException.
        assertThat(TourDurationParser.parseDays("99999999999 days")).isEmpty();
    }
}
