package com.epam.edp.demo.validation.review;

import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.dto.review.UpdateReviewRequestDTO;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.exception.tour.TourValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourReviewValidatorTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-06-16");

    private final TourReviewValidator validator = new TourReviewValidator();

    @Test
    void validateCreateRequest_passesForStartedBooking() {
        Booking booking = booking("user-1", "2026-06-01", "10 days", BookingStatus.STARTED);

        validator.validateCreateRequest("user-1", booking, new CreateReviewRequestDTO(5, "Great"), TODAY);
    }

    @Test
    void validateCreateRequest_passesForFinishedBooking() {
        Booking booking = booking("user-1", "2026-05-01", "10 days", BookingStatus.FINISHED);

        validator.validateCreateRequest("user-1", booking, new CreateReviewRequestDTO(5, null), TODAY);
    }

    @Test
    void validateCreateRequest_failsWhenCallerIsNotOwner() {
        Booking booking = booking("owner-1", "2026-06-01", "10 days", BookingStatus.STARTED);

        assertThatThrownBy(() ->
                validator.validateCreateRequest("other-user", booking, new CreateReviewRequestDTO(5, null), TODAY))
                .isInstanceOf(TourValidationException.class)
                .satisfies(e -> assertThat(((TourValidationException) e).getErrors())
                        .containsEntry("caller", "must be booking owner"));
    }

    @Test
    void validateCreateRequest_failsWhenBookingNotStartedOrFinished() {
        Booking booking = booking("user-1", "2026-07-01", "10 days", BookingStatus.BOOKED);

        assertThatThrownBy(() ->
                validator.validateCreateRequest("user-1", booking, new CreateReviewRequestDTO(5, null), TODAY))
                .isInstanceOf(TourValidationException.class)
                .satisfies(e -> assertThat(((TourValidationException) e).getErrors())
                        .containsEntry("booking", "must be STARTED or FINISHED"));
    }

    @Test
    void validateCreateRequest_passesWhenLowRateWithContent() {
        Booking booking = booking("user-1", "2026-06-01", "10 days", BookingStatus.STARTED);

        validator.validateCreateRequest("user-1", booking, new CreateReviewRequestDTO(3, "Could be better"), TODAY);
    }

    @Test
    void validateUpdateRequest_failsWhenBothRateAndContentNull() {
        Booking booking = booking("user-1", "2026-06-01", "10 days", BookingStatus.STARTED);
        Review review = review("user-1", 5, "Good");

        assertThatThrownBy(() ->
                validator.validateUpdateRequest("user-1", review, booking, new UpdateReviewRequestDTO(null, null), TODAY))
                .isInstanceOf(TourValidationException.class)
                .satisfies(e -> assertThat(((TourValidationException) e).getErrors())
                        .containsEntry("body", "at least one of rate or reviewContent required"));
    }

    @Test
    void validateUpdateRequest_failsWhenCallerNotAuthor() {
        Booking booking = booking("user-1", "2026-06-01", "10 days", BookingStatus.STARTED);
        Review review = review("author-1", 5, "Good");

        assertThatThrownBy(() ->
                validator.validateUpdateRequest("user-1", review, booking, new UpdateReviewRequestDTO(4, null), TODAY))
                .isInstanceOf(TourValidationException.class)
                .satisfies(e -> assertThat(((TourValidationException) e).getErrors())
                        .containsEntry("caller", "must be review author"));
    }

    @Test
    void validateUpdateRequest_failsWhenBookingNotStartedOrFinished() {
        Booking booking = booking("user-1", "2026-07-01", "10 days", BookingStatus.BOOKED);
        Review review = review("user-1", 5, "Good");

        assertThatThrownBy(() ->
                validator.validateUpdateRequest("user-1", review, booking, new UpdateReviewRequestDTO(4, null), TODAY))
                .isInstanceOf(TourValidationException.class)
                .satisfies(e -> assertThat(((TourValidationException) e).getErrors())
                        .containsEntry("booking", "must be STARTED or FINISHED"));
    }

    @Test
    void validateUpdateRequest_requiresContentWhenLoweringRateBelowFour() {
        Booking booking = booking("user-1", "2026-06-01", "10 days", BookingStatus.STARTED);
        Review review = review("user-1", 5, null);

        assertThatThrownBy(() ->
                validator.validateUpdateRequest("user-1", review, booking, new UpdateReviewRequestDTO(2, null), TODAY))
                .isInstanceOf(TourValidationException.class)
                .satisfies(e -> assertThat(((TourValidationException) e).getErrors())
                        .containsEntry("reviewContent", "required when rate is 1-3"));
    }

    @Test
    void validateUpdateRequest_passesWhenOnlyContentChanged() {
        Booking booking = booking("user-1", "2026-06-01", "10 days", BookingStatus.STARTED);
        Review review = review("user-1", 5, "old");

        validator.validateUpdateRequest("user-1", review, booking, new UpdateReviewRequestDTO(null, "new"), TODAY);
    }

    private static Booking booking(String userId, String startDate, String duration, BookingStatus state) {
        Booking b = new Booking();
        b.setUserId(userId);
        b.setStartDate(LocalDate.parse(startDate));
        b.setDuration(duration);
        b.setState(state);
        return b;
    }

    private static Review review(String userId, int rate, String content) {
        Review r = new Review();
        r.setUserId(userId);
        r.setRate(rate);
        r.setReviewContent(content);
        return r;
    }
}
