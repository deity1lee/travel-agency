package com.epam.edp.demo.dto.bookings;

import java.util.List;

public record BookingResponseCustomerDto(
         String name,
         String email,
         List<PersonalDetailsResponse> guests,
         String paymentDocument
) {
}
