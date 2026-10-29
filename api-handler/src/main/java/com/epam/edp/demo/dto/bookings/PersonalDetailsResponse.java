package com.epam.edp.demo.dto.bookings;

public record PersonalDetailsResponse(
        String firstName,
        String lastName,
        String passportDocumentId
) {
}
