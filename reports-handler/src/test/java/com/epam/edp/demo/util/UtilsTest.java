package com.epam.edp.demo.util;

import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.entity.BookingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
class UtilsTest {

    @Test
    void test_getCityCountry_returnsCityAndCountry_whenDestinationContainsComma() {
        String[] result = Utils.getCityCountry("Paris, France");

        assertEquals("Paris", result[0]);
        assertEquals("France", result[1]);
    }

    @Test
    void test_getCityCountry_returnsOnlyCityAndEmptyCountry_whenDestinationHasNoComma() {
        String[] result = Utils.getCityCountry("Paris");

        assertEquals("Paris", result[0]);
        assertEquals("", result[1]);
    }

    @Test
    void test_getCityCountry_preservesTokenWhitespace_whenDestinationHasExtraWhitespaceInsideTokens() {
        String[] result = Utils.getCityCountry("  Rome  ,   Italy   ");

        assertEquals("  Rome", result[0]);
        assertEquals("Italy   ", result[1]);
    }

    @Test
    void test_nvl_returnsZero_whenInputIsNull() {
        BigDecimal result = Utils.nvl(null);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void test_nvl_returnsSameValue_whenInputIsNotNull() {
        BigDecimal value = BigDecimal.valueOf(42.5);

        BigDecimal result = Utils.nvl(value);

        assertEquals(value, result);
    }

    @Test
    void test_getLoss_returnsZero_whenStatusIsConfirmed() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.CONFIRMED, 100, 10.0);

        BigDecimal result = Utils.getLoss(event);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void test_getLoss_returnsZero_whenStatusIsFinished() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.FINISHED, 100, 10.0);

        BigDecimal result = Utils.getLoss(event);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void test_getLoss_returnsDifferenceBetweenPriceAndCancelCharge_whenStatusIsCanceled() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.CANCELED, 100, 35.5);

        BigDecimal result = Utils.getLoss(event);

        assertEquals(0, result.compareTo(BigDecimal.valueOf(64.5)));
    }

    @Test
    void test_getLoss_returnsZero_whenStatusFallsIntoDefaultBranch() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.BOOKED, 100, 10.0);

        BigDecimal result = Utils.getLoss(event);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void test_getRevenue_returnsPrice_whenStatusIsConfirmed() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.CONFIRMED, 250, 10.0);

        BigDecimal result = Utils.getRevenue(event);

        assertEquals(0, result.compareTo(BigDecimal.valueOf(250)));
    }

    @Test
    void test_getRevenue_returnsPrice_whenStatusIsFinished() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.FINISHED, 300, 10.0);

        BigDecimal result = Utils.getRevenue(event);

        assertEquals(0, result.compareTo(BigDecimal.valueOf(300)));
    }

    @Test
    void test_getRevenue_returnsCancelCharge_whenStatusIsCanceled() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.CANCELED, 100, 15.75);

        BigDecimal result = Utils.getRevenue(event);

        assertEquals(0, result.compareTo(BigDecimal.valueOf(15.75)));
    }

    @Test
    void test_getRevenue_returnsZero_whenStatusFallsIntoDefaultBranch() {
        BookingStatusChangedEvent event = bookingEvent(BookingStatus.STARTED, 100, 10.0);

        BigDecimal result = Utils.getRevenue(event);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void test_deltaBigDecimal_returnsPercentDelta_whenPreviousIsNonZero() {
        BigDecimal result = Utils.delta(BigDecimal.valueOf(120), BigDecimal.valueOf(100));

        assertEquals(0, result.compareTo(BigDecimal.valueOf(20.0000)));
    }

    @Test
    void test_deltaBigDecimal_returnsNull_whenPreviousIsNull() {
        BigDecimal result = Utils.delta(BigDecimal.valueOf(120), null);

        assertNull(result);
    }

    @Test
    void test_deltaBigDecimal_returnsNull_whenPreviousIsZero() {
        BigDecimal result = Utils.delta(BigDecimal.valueOf(120), BigDecimal.ZERO);

        assertNull(result);
    }

    @Test
    void test_deltaInteger_returnsPercentDelta_whenPreviousIsNonZero() {
        BigDecimal result = Utils.delta(90, 60);

        assertEquals(0, result.compareTo(BigDecimal.valueOf(50.0000)));
    }

    @Test
    void test_deltaInteger_returnsNull_whenPreviousIsNull() {
        BigDecimal result = Utils.delta(10, null);

        assertNull(result);
    }

    @Test
    void test_deltaInteger_returnsNull_whenPreviousIsZero() {
        BigDecimal result = Utils.delta(10, 0);

        assertNull(result);
    }

    private BookingStatusChangedEvent bookingEvent(BookingStatus status, int price, double cancelCharge) {
        return BookingStatusChangedEvent.builder()
                .bookingStatus(status)
                .price(price)
                .cancelCharge(cancelCharge)
                .build();
    }
}




