package com.epam.edp.demo.dto.reports;

import com.epam.edp.demo.entity.BookingStatus;
import lombok.Builder;


@Builder
public record BookingStatusChangedEvent(

    String tourId,
    String bookingId,
    BookingStatus bookingStatus,

    String travelAgentName,
    String travelAgentEmail,

    String tourName,
    String destination,

    String tourEndDate,
    String tourStatDate,

    int price,
    double cancelCharge
) {}
