package com.epam.edp.demo.dto.bookings;

import java.util.List;

public record BookingListResponse(
        List<BookingDto> bookings
) {
}
