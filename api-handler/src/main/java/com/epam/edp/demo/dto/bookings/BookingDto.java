package com.epam.edp.demo.dto.bookings;

import com.epam.edp.demo.entity.booking.TravelAgentSnapshot;
import com.epam.edp.demo.entity.tourModel.TourType;

import java.time.LocalDate;

public record BookingDto(
         String id,
         String tourId,
         String state,
         String tourImageUrl,
         String name,
         String destination,
         TourType tourType,
         BookedTourDetailsDto tourDetails,
         TravelAgentSnapshot travelAgent,
         BookingResponseCustomerDto customer,
         LocalDate freeCancellation,
         String canceledBy,
         String cancelReason,
         BookingReviewDto review
) {
}
