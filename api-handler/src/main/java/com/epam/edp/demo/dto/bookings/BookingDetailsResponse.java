package com.epam.edp.demo.dto.bookings;

import io.swagger.v3.oas.annotations.media.Schema;

public record BookingDetailsResponse(
        @Schema(description = "Free cancellation date of the booking", example = "2027-02-19")
        String freeCancellation,

        @Schema(description = "Id of tour to be booked", example = "You have booked tour Tropical Caribe starting date 2027-03-01 (12 days), BB for 1 adult(s) and 0 children successfully. Please upload your travel documents to the booking on the 'My Tours' page and wait for the Travel Agent to contact you.")
        String details
) {
}
