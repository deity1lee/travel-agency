package com.epam.edp.demo.util;

import com.epam.edp.demo.dto.bookings.BookingRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class BookingCalculationsUtils {

    public static String calculateFreeCancellationDate(String startDate) {
        LocalDate date = LocalDate.parse(startDate);
        LocalDate freeCancellationDate = date.minusDays(10);
        return freeCancellationDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate calculateFreeCancellationDate(LocalDate startDate) {
        return startDate.minusDays(10);
    }

    public static String formatBookingCreateResponseDetails(BookingRequest dto, String tourName,String travelAgentName) {

        int adults = dto.guests().getAdults();
        int children = dto.guests().getChildren();

        return String.format(
                "You have booked tour %s starting date %s (%s), %s for %d adult(s) and %d children successfully. " +
                        "Please upload your travel documents to the booking on the 'My Tours' page and wait for Travel Agent %s to contact you.",
                tourName,
                dto.date(),
                dto.duration(),
                dto.mealPlan(),
                adults,
                children,
                travelAgentName
        );
    }

    public static String formatBookingEditResponseDetails(BookingRequest dto, String tourName) {

        int adults = dto.guests().getAdults();
        int children = dto.guests().getChildren();

        return String.format(
                "You have edited your booking for tour %s to starting date %s (%s), %s for %d adult(s) and %d children successfully. " +
                        "Please upload your travel documents to the booking on the 'My Tours' page and wait for the Travel Agent to contact you.",
                tourName,
                dto.date(),
                dto.duration(),
                dto.mealPlan(),
                adults,
                children
        );
    }

    public static int calculateCancellationFee(LocalDate date, int price) {
        long daysUntilTour = ChronoUnit.DAYS.between(LocalDate.now(), date);

        if (daysUntilTour >= 10) {
            return 0;
        }

        if (daysUntilTour >= 3) {
            return price / 2;
        }

        return price;
    }

    public static int calculateBookingPrice(int guestsNumber, int price, Integer duration, Integer mealPricePerDay) {
        return Math.max(1, guestsNumber) *
                (price + (duration == null ? 0 : duration) *
                        (mealPricePerDay == null ? 0 : mealPricePerDay));
    }

}
