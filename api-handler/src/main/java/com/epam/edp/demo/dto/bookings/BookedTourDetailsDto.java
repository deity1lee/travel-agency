package com.epam.edp.demo.dto.bookings;

public record BookedTourDetailsDto(
        String date,
        String duration,
        String mealPlan,
        String totalPrice
) {
}
