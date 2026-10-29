package com.epam.edp.demo.util;

import com.epam.edp.demo.dto.bookings.BookingRequest;
import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.tourModel.MealPlan;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingCalculationsUtilsTest {

    private static BookingRequest request(int adults, int children, String date, String duration, MealPlan mealPlan) {
        Guests guests = new Guests();
        guests.setAdults(adults);
        guests.setChildren(children);
        return new BookingRequest("tour-1", LocalDate.parse(date), duration, mealPlan, guests, List.of());
    }

    @Test
    void calculateFreeCancellationDate_string_subtractsTenDays() {
        assertThat(BookingCalculationsUtils.calculateFreeCancellationDate("2027-03-01"))
                .isEqualTo("2027-02-19");
    }

    @Test
    void calculateFreeCancellationDate_localDate_subtractsTenDays() {
        assertThat(BookingCalculationsUtils.calculateFreeCancellationDate(LocalDate.of(2027, 3, 1)))
                .isEqualTo(LocalDate.of(2027, 2, 19));
    }

    @Test
    void formatBookingCreateResponseDetails_includesAllFieldsAndAgentName() {
        BookingRequest req = request(2, 1, "2027-03-01", "12 days", MealPlan.BB);
        String details = BookingCalculationsUtils.formatBookingCreateResponseDetails(req, "Tropical Caribe", "Jane Agent");

        assertThat(details).contains(
                "Tropical Caribe",
                "2027-03-01",
                "12 days",
                "BB",
                "2 adult(s)",
                "1 children",
                "Jane Agent");
    }

    @Test
    void formatBookingEditResponseDetails_includesAllFieldsWithoutAgentName() {
        BookingRequest req = request(3, 0, "2027-03-01", "7 days", MealPlan.HB);
        String details = BookingCalculationsUtils.formatBookingEditResponseDetails(req, "Tropical Caribe");

        assertThat(details).contains(
                "Tropical Caribe",
                "2027-03-01",
                "7 days",
                "HB",
                "3 adult(s)",
                "0 children");
    }

    @Test
    void calculateCancellationFee_returnsZero_whenTenOrMoreDaysAway() {
        LocalDate inTenDays = LocalDate.now().plusDays(10);
        assertThat(BookingCalculationsUtils.calculateCancellationFee(inTenDays, 1000)).isZero();
    }

    @Test
    void calculateCancellationFee_returnsHalfPrice_whenBetweenThreeAndNineDaysAway() {
        LocalDate inFiveDays = LocalDate.now().plusDays(5);
        assertThat(BookingCalculationsUtils.calculateCancellationFee(inFiveDays, 1000)).isEqualTo(500);
    }

    @Test
    void calculateCancellationFee_returnsFullPrice_whenLessThanThreeDaysAway() {
        LocalDate inTwoDays = LocalDate.now().plusDays(2);
        assertThat(BookingCalculationsUtils.calculateCancellationFee(inTwoDays, 1000)).isEqualTo(1000);
    }

    @Test
    void calculateCancellationFee_returnsFullPrice_whenInThePast() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThat(BookingCalculationsUtils.calculateCancellationFee(yesterday, 1000)).isEqualTo(1000);
    }

    @Test
    void calculateBookingPrice_withMealPlan_returnsGuestsTimesPriceWithMealSupplement() {
        // 3 guests * (1000 + 12 days * 20 meal price) = 3 * 1240 = 3720
        assertThat(BookingCalculationsUtils.calculateBookingPrice(3, 1000, 12, 20)).isEqualTo(3720);
    }

    @Test
    void calculateBookingPrice_treatsNullDurationAndMealPriceAsZero() {
        assertThat(BookingCalculationsUtils.calculateBookingPrice(2, 500, null, null)).isEqualTo(1000);
        assertThat(BookingCalculationsUtils.calculateBookingPrice(2, 500, 7, null)).isEqualTo(1000);
        assertThat(BookingCalculationsUtils.calculateBookingPrice(2, 500, null, 20)).isEqualTo(1000);
    }

    @Test
    void calculateBookingPrice_treatsZeroGuestsAsOne() {
        assertThat(BookingCalculationsUtils.calculateBookingPrice(0, 500, 5, 10)).isEqualTo(550);
    }
}
