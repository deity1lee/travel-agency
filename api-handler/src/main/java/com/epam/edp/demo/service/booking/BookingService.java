package com.epam.edp.demo.service.booking;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.bookings.*;
import com.epam.edp.demo.entity.booking.BookingStatus;


public interface BookingService {
    BookingListResponse getBookings(UserContext userContext, BookingStatus status);
    BookingDetailsResponse createBooking(UserContext userContext, BookingRequest request);
    BookingCancellationResponse cancelBooking(UserContext userContext, String bookingId, CancelBookingRequest request);
    BookingDetailsResponse editBooking(UserContext userContext, String bookingId, BookingRequest request);
    void confirmBooking(UserContext userContext, String bookingId);
}
