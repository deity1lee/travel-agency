package com.epam.edp.demo.service.reports;

import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.dto.reports.FeedbackEvent;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.review.Review;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventMapper {

    private EventMapper(){}

    public static BookingStatusChangedEvent toBookingStateTransitionEvent(Booking booking) {
        log.info("Mapping booking {} to state transition event", booking.getBookingId());

        BookingStatusChangedEvent sqsEvent = new BookingStatusChangedEvent(
                booking.getTourId(),
                booking.getBookingId(),
                booking.getState(),
                booking.getTravelAgent().getName(), booking.getTravelAgent().getEmail(),
                booking.getName(),
                booking.getDestination(),
                booking.getStartDate().plusDays(Long.parseLong(booking.getDuration().split(" ")[0])).toString(),
                booking.getStartDate().toString(),
                booking.getTotalPrice(),
                booking.getCharged()
        );

        return sqsEvent;
    }

    public static FeedbackEvent toFeedbackEvent(Review review, Integer newRate, Integer oldRate, Booking booking) {
        log.info("Mapping Review {} to state transition event", review.getReviewId());

        return new FeedbackEvent(
                review.getCreatedAt(),
                newRate,
                oldRate,
                review.getTourId(),
                booking.getTravelAgentEmail(),
                booking.getTravelAgent().getName(),
                booking.getDestination(),
                booking.getName()
        );
    }
}
