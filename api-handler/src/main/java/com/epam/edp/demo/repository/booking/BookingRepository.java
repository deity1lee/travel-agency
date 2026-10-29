package com.epam.edp.demo.repository.booking;

import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    void save(Booking booking);

    List<Booking> findAll();

    boolean anyExists();

    Optional<Booking> findByBookingId(String bookingId);

    List<Booking> getBookingsByCustomerId(String customerId);

    List<Booking> getBookingsByCustomerIdFilterStatus(String customerId, BookingStatus bookingStatus);

    List<Booking> getBookingsByTravelAgentEmail(String travelAgentEmail);

    List<Booking> getBookingsByTravelAgentEmailFilterStatus(String travelAgentEmail, BookingStatus bookingStatus);

    void cancelBooking(Booking booking);

    void cancelBookingByTravelAgent(Booking booking, String reason);

    void saveBookings(List<Booking> bookings);
}
