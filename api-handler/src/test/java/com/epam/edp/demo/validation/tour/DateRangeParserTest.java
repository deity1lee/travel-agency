package com.epam.edp.demo.validation.tour;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeParserTest {

    @Test
    void parse_validRange_returnsSuccess() {
        DateRangeParser.Result result = DateRangeParser.parse("Apr 1 2026 - Apr 15 2026");

        assertTrue(result.isSuccess());
        assertNull(result.getFailure());
        assertEquals(LocalDate.of(2026, 4, 1), result.getDateRange().getFrom());
        assertEquals(LocalDate.of(2026, 4, 15), result.getDateRange().getTo());
    }

    @Test
    void parse_missingSeparator_returnsUnparseable() {
        DateRangeParser.Result result = DateRangeParser.parse("Apr 1 2026");

        assertFalse(result.isSuccess());
        assertEquals(DateRangeParser.Failure.UNPARSEABLE, result.getFailure());
    }

    @Test
    void parse_badMonthAbbreviation_returnsUnparseable() {
        DateRangeParser.Result result = DateRangeParser.parse("Xxx 1 2026 - Apr 15 2026");

        assertFalse(result.isSuccess());
        assertEquals(DateRangeParser.Failure.UNPARSEABLE, result.getFailure());
    }

    @Test
    void parse_fromAfterTo_returnsFromAfterTo() {
        DateRangeParser.Result result = DateRangeParser.parse("Apr 15 2026 - Apr 1 2026");

        assertFalse(result.isSuccess());
        assertEquals(DateRangeParser.Failure.FROM_AFTER_TO, result.getFailure());
    }

    @Test
    void parse_nullInput_returnsUnparseable() {
        DateRangeParser.Result result = DateRangeParser.parse(null);

        assertFalse(result.isSuccess());
        assertEquals(DateRangeParser.Failure.UNPARSEABLE, result.getFailure());
    }
}
