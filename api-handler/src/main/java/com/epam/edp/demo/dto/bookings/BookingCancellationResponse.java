package com.epam.edp.demo.dto.bookings;

public record BookingCancellationResponse(
        String message,
        double chargedAmount
){}
