package com.epam.edp.demo.service.reports;

import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.dto.reports.FeedbackEvent;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.booking.TravelAgentSnapshot;
import com.epam.edp.demo.entity.review.Review;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventMapperServiceTest {

    @Test
    void toBookingStateTransitionEvent_copiesScalarFields() {
        Booking booking = Booking.builder()
                .bookingId("b-1")
                .tourId("tour-1")
                .state(BookingStatus.CONFIRMED)
                .name("Paris")
                .destination("France")
                .startDate(LocalDate.of(2027, 3, 1))
                .duration("7 days")
                .totalPrice(1500)
                .charged(0.0)
                .travelAgent(new TravelAgentSnapshot("Jane Agent", "jane@x.com", "555", "tg"))
                .build();

        BookingStatusChangedEvent event = EventMapper.toBookingStateTransitionEvent(booking);

        assertThat(event.tourId()).isEqualTo("tour-1");
        assertThat(event.bookingId()).isEqualTo("b-1");
        assertThat(event.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(event.travelAgentName()).isEqualTo("Jane Agent");
        assertThat(event.travelAgentEmail()).isEqualTo("jane@x.com");
        assertThat(event.tourName()).isEqualTo("Paris");
        assertThat(event.destination()).isEqualTo("France");
        assertThat(event.tourStatDate()).isEqualTo("2027-03-01");
        assertThat(event.tourEndDate()).isEqualTo("2027-03-08");
        assertThat(event.price()).isEqualTo(1500);
        assertThat(event.cancelCharge()).isEqualTo(0.0);
    }

    @Test
    void toBookingStateTransitionEvent_endDateUsesFirstDurationNumber() {
        Booking booking = Booking.builder()
                .bookingId("b-2")
                .tourId("tour-1")
                .state(BookingStatus.CONFIRMED)
                .name("Long Trip")
                .destination("X")
                .startDate(LocalDate.of(2027, 1, 1))
                .duration("14 days adventure")
                .totalPrice(0)
                .charged(0.0)
                .travelAgent(new TravelAgentSnapshot("Agent", "a@x.com", "1", "t"))
                .build();

        BookingStatusChangedEvent event = EventMapper.toBookingStateTransitionEvent(booking);

        assertThat(event.tourEndDate()).isEqualTo("2027-01-15");
    }

    @Test
    void toBookingStateTransitionEvent_propagatesCharged() {
        Booking booking = baseBooking().charged(123.45).build();

        BookingStatusChangedEvent event = EventMapper.toBookingStateTransitionEvent(booking);

        assertThat(event.cancelCharge()).isEqualTo(123.45);
    }

    @Test
    void toBookingStateTransitionEvent_missingTravelAgent_throwsNPE() {
        Booking booking = baseBooking().travelAgent(null).build();

        assertThatThrownBy(() -> EventMapper.toBookingStateTransitionEvent(booking))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toFeedbackEvent_copiesAllFields() {
        Review review = new Review();
        review.setReviewId("r-1");
        review.setTourId("tour-1");
        review.setCreatedAt("2027-01-01T00:00:00Z");

        Booking booking = Booking.builder()
                .name("Paris")
                .destination("France")
                .travelAgentEmail("jane@x.com")
                .travelAgent(new TravelAgentSnapshot("Jane Agent", "jane@x.com", "555", "tg"))
                .build();

        FeedbackEvent event = EventMapper.toFeedbackEvent(review, 5, 3, booking);

        assertThat(event.createdAt()).isEqualTo("2027-01-01T00:00:00Z");
        assertThat(event.newRate()).isEqualTo(5);
        assertThat(event.oldRate()).isEqualTo(3);
        assertThat(event.tourId()).isEqualTo("tour-1");
        assertThat(event.travelAgentEmail()).isEqualTo("jane@x.com");
        assertThat(event.travelAgentName()).isEqualTo("Jane Agent");
        assertThat(event.destination()).isEqualTo("France");
        assertThat(event.tourName()).isEqualTo("Paris");
    }

    @Test
    void toFeedbackEvent_oldRateNullAllowed() {
        Review review = new Review();
        review.setReviewId("r-1");
        review.setTourId("tour-1");
        review.setCreatedAt("2027-01-01T00:00:00Z");

        Booking booking = Booking.builder()
                .name("Paris")
                .destination("France")
                .travelAgentEmail("jane@x.com")
                .travelAgent(new TravelAgentSnapshot("Jane Agent", "jane@x.com", "555", "tg"))
                .build();

        FeedbackEvent event = EventMapper.toFeedbackEvent(review, 4, null, booking);

        assertThat(event.oldRate()).isNull();
        assertThat(event.newRate()).isEqualTo(4);
    }

    private static Booking.BookingBuilder baseBooking() {
        return Booking.builder()
                .bookingId("b-1")
                .tourId("tour-1")
                .state(BookingStatus.CONFIRMED)
                .name("Paris")
                .destination("France")
                .startDate(LocalDate.of(2027, 3, 1))
                .duration("7 days")
                .totalPrice(1500)
                .charged(0.0)
                .travelAgent(new TravelAgentSnapshot("Jane Agent", "jane@x.com", "555", "tg"));
    }
}
