package com.epam.edp.demo.service.review;

import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.dto.review.ReviewListResponseDTO;
import com.epam.edp.demo.dto.review.ReviewResponseDTO;
import com.epam.edp.demo.dto.review.UpdateReviewRequestDTO;
import com.epam.edp.demo.dto.moderation.ModerationResult;
import com.epam.edp.demo.dto.moderation.ModerationStatus;
import com.epam.edp.demo.dto.tour.TourReviewQueryDTO;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.booking.TravelAgentSnapshot;
import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.entity.tourModel.ReviewSortBy;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.review.ReviewConflictException;
import com.epam.edp.demo.exception.review.ReviewModerationRejectedException;
import com.epam.edp.demo.exception.review.ReviewNotFoundException;
import com.epam.edp.demo.exception.tour.TourValidationException;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.exception.review.DuplicateReviewException;
import com.epam.edp.demo.exception.review.OptimisticLockException;
import com.epam.edp.demo.repository.review.RatingAggregateUpdate;
import com.epam.edp.demo.repository.review.RatingAggregates;
import com.epam.edp.demo.repository.review.ReviewRepository;
import com.epam.edp.demo.service.document.S3PresignedUrlService;
import com.epam.edp.demo.service.moderation.ModerationService;
import com.epam.edp.demo.service.reports.EventPublisherService;
import com.epam.edp.demo.util.JitteredBackoff;
import com.epam.edp.demo.validation.review.TourReviewValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.beans.Beans.isInstanceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReviewServiceImplTest {

    private static final LocalDate NOW = LocalDate.parse("2026-06-16");
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(
            NOW.atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC
    );
    private final JitteredBackoff backoff = new JitteredBackoff(millis -> {});
    private ReviewServiceImpl service;
    private final EventPublisherService eventPublisherService = mock(EventPublisherService.class);
    private final ModerationService moderationService = mock(ModerationService.class);
    private final S3PresignedUrlService presignedUrlService = mock(S3PresignedUrlService.class);


    @BeforeEach
    void setUp() {
        when(moderationService.moderate(any())).thenReturn(ModerationResult.approved("ok"));
        service = new ReviewServiceImpl(reviewRepository, bookingRepository, userRepository, clock, backoff, eventPublisherService, moderationService, presignedUrlService);
    }

    @Test
    void getTourReviews_delegatesToRepositoryAndMaps() {
        TourReviewQueryDTO request = new TourReviewQueryDTO(1, 4, ReviewSortBy.RATING_DESC);
        Review review = review("tour-1", "review-1", "Jane", 5, "Great");
        when(reviewRepository.getReviewsByTourId("tour-1", 1, 4, ReviewSortBy.RATING_DESC))
                .thenReturn(List.of(review));
        when(reviewRepository.getReviewCountForTour("tour-1")).thenReturn(1);

        ReviewListResponseDTO response = service.getTourReviews("tour-1", request);

        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().get(0).authorName()).isEqualTo("Jane");
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(4);
    }

    @Test
    void getTourReviews_returnsEmptyListWhenNoReviews() {
        TourReviewQueryDTO request = new TourReviewQueryDTO(1, 4, ReviewSortBy.NEWEST);
        when(reviewRepository.getReviewsByTourId("tour-1", 1, 4, ReviewSortBy.NEWEST))
                .thenReturn(List.of());
        when(reviewRepository.getReviewCountForTour("tour-1")).thenReturn(0);

        ReviewListResponseDTO response = service.getTourReviews("tour-1", request);

        assertThat(response.reviews()).isEmpty();
        assertThat(response.totalItems()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
    }

    @Test
    void createReview_throwsBookingNotFoundWhenBookingMissing() {
        when(bookingRepository.findByBookingId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReview("missing", "user-1", new CreateReviewRequestDTO(5, "Good")))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("booking not found");
    }

    @Test
    void createReview_throwsBookingNotFoundWhenUserMissing() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Good")))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("user not found");
    }

    @Test
    void createReview_propagatesValidationException() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));

        try (MockedStatic<TourReviewValidator> validator = mockStatic(TourReviewValidator.class)) {
            validator.when(() -> TourReviewValidator.validateCreateRequest(
                            eq("user-1"), eq(booking), any(), any()))
                    .thenThrow(new TourValidationException(Map.of("rate", "must be between 1 and 5")));

            assertThatThrownBy(() -> service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(0, "x")))
                    .isInstanceOf(TourValidationException.class);
        }
    }

    @Test
    void createReview_persistsReviewAndAggregateUpdateOnSuccess() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(reviewRepository.getRatingAggregates("tour-1")).thenReturn(new RatingAggregates(20L, 5, 4.0));

        ReviewResponseDTO response = service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Great"));

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        ArgumentCaptor<RatingAggregateUpdate> aggregateCaptor = ArgumentCaptor.forClass(RatingAggregateUpdate.class);
        verify(reviewRepository).createReview(reviewCaptor.capture(), aggregateCaptor.capture());

        Review saved = reviewCaptor.getValue();
        assertThat(saved.getTourId()).isEqualTo("tour-1");
        assertThat(saved.getReviewId()).isEqualTo("booking-1");
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getAuthorName()).isEqualTo("Jane Doe");
        assertThat(saved.getAuthorImageUrl()).isNull();
        assertThat(saved.getCreatedAt()).isEqualTo(NOW.toString());
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW.toString());
        assertThat(saved.getRate()).isEqualTo(5);
        assertThat(saved.getReviewContent()).isEqualTo("Great");

        RatingAggregateUpdate update = aggregateCaptor.getValue();
        assertThat(update.expectedRatingSum()).isEqualTo(20L);
        assertThat(update.expectedReviews()).isEqualTo(5);
        assertThat(update.newRatingSum()).isEqualTo(25L);
        assertThat(update.newReviews()).isEqualTo(6);
        assertThat(update.newRating()).isEqualTo(round2(25.0 / 6));

        assertThat(response.authorName()).isEqualTo("Jane Doe");
        assertThat(response.rate()).isEqualTo(5);
        assertThat(response.reviewContent()).isEqualTo("Great");
    }

    @Test
    void createReview_moderatesSubmittedTextAfterValidationBeforePersistence() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(reviewRepository.getRatingAggregates("tour-1")).thenReturn(new RatingAggregates(20L, 5, 4.0));

        service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Harsh but civil"));

        verify(moderationService).moderate("Harsh but civil");
        verify(reviewRepository).createReview(any(), any());
    }

    @Test
    void createReview_blankContentWithHighRate_skipsModerationAndPersists() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(reviewRepository.getRatingAggregates("tour-1")).thenReturn(new RatingAggregates(20L, 5, 4.0));

        ReviewResponseDTO response = service.createReview("booking-1", "user-1",
                new CreateReviewRequestDTO(5, null));

        verify(moderationService, never()).moderate(any());
        verify(reviewRepository).createReview(any(), any());
        assertThat(response.rate()).isEqualTo(5);
    }

    @Test
    void createReview_rejectedByModeration_throwsAndDoesNotPersist() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(moderationService.moderate(any())).thenReturn(new ModerationResult(
                ModerationStatus.REJECTED, "contains a threat", List.of("THREAT"),
                "Please remove threatening language and resubmit."));

        assertThatThrownBy(() -> service.createReview("booking-1", "user-1",
                new CreateReviewRequestDTO(5, "I will hurt you")))
                .isInstanceOf(ReviewModerationRejectedException.class)
                .hasMessage("contains a threat");

        verify(reviewRepository, never()).createReview(any(), any());
        verifyNoInteractions(eventPublisherService);
    }

    @Test
    void createReview_throwsConflictWhenReviewAlreadyExists() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(reviewRepository.getRatingAggregates("tour-1")).thenReturn(new RatingAggregates(0L, 0, 0.0));
        doThrow(new DuplicateReviewException("review already exists for booking booking-1"))
                .when(reviewRepository).createReview(any(), any());

        assertThatThrownBy(() -> service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Great")))
                .isInstanceOf(ReviewConflictException.class)
                .hasMessage("A review already exists for this booking. You can only update it.");
        verify(reviewRepository, times(1)).createReview(any(), any());
    }

    @Test
    void createReview_throwsConflictWhenReviewAlreadyExists_withoutModeratingOrInserting() {
        // A booking already carries a review: the second create must short-circuit to a 409
        // conflict before moderation or the transactional insert, not fall through to a 404.
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = new Review();
        existing.setReviewId("booking-1");
        existing.setTourId("tour-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Great")))
                .isInstanceOf(ReviewConflictException.class)
                .hasMessage("A review already exists for this booking. You can only update it.");
        verify(moderationService, never()).moderate(any());
        verify(reviewRepository, never()).createReview(any(), any());
    }

    @Test
    void createReview_retriesAndSucceedsWhenAggregateOptimisticLockFails() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0))
                .thenReturn(new RatingAggregates(25L, 6, 4.16));

        doThrow(new OptimisticLockException("tour rating aggregate changed"))
                .doNothing().when(reviewRepository).createReview(any(), any());

        ReviewResponseDTO response = service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Great"));

        verify(reviewRepository, times(2)).createReview(any(), any());
        verify(reviewRepository, times(2)).getRatingAggregates("tour-1");
        assertThat(response.rate()).isEqualTo(5);
    }

    @Test
    void createReview_throwsConflictAfterMaxRetries() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(reviewRepository.getRatingAggregates("tour-1")).thenReturn(new RatingAggregates(20L, 5, 4.0));

        doThrow(new OptimisticLockException("tour rating aggregate changed"))
                .when(reviewRepository).createReview(any(), any());

        assertThatThrownBy(() -> service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Great")))
                .isInstanceOf(ReviewConflictException.class)
                .hasMessage("conflict, please retry");
        verify(reviewRepository, times(3)).createReview(any(), any());
    }

    @Test
    void createReview_propagatesUnexpectedRepositoryException() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        User user = user("user-1", "Jane", "Doe");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(reviewRepository.getRatingAggregates("tour-1")).thenReturn(new RatingAggregates(0L, 0, 0.0));
        // Anything that is neither a duplicate nor an optimistic-lock failure propagates unchanged.
        doThrow(new IllegalStateException("mongo down")).when(reviewRepository).createReview(any(), any());

        assertThatThrownBy(() -> service.createReview("booking-1", "user-1", new CreateReviewRequestDTO(5, "Great")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mongo down");
        verify(reviewRepository, times(1)).createReview(any(), any());
    }

    @Test
    void updateReview_throwsBookingNotFoundWhenBookingMissing() {
        when(bookingRepository.findByBookingId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview("missing", "user-1", new UpdateReviewRequestDTO(5, "x")))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("booking not found");
    }

    @Test
    void updateReview_throwsReviewNotFoundWhenReviewMissing() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1", new UpdateReviewRequestDTO(5, "x")))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessage("review not found");
    }

    @Test
    void updateReview_propagatesValidationException() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 4, "ok");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));

        try (MockedStatic<TourReviewValidator> validator = mockStatic(TourReviewValidator.class)) {
            validator.when(() -> TourReviewValidator.validateUpdateRequest(
                            eq("user-1"), eq(existing), eq(booking), any(), any()))
                    .thenThrow(new TourValidationException(Map.of("rate", "must be between 1 and 5")));

            assertThatThrownBy(() -> service.updateReview("booking-1", "user-1", new UpdateReviewRequestDTO(7, "x")))
                    .isInstanceOf(TourValidationException.class);
        }

//        doThrow(new TourValidationException(java.util.Map.of("rate", "must be between 1 and 5")))
//                .when(validator).validateUpdateRequest(eq("user-1"), eq(existing), eq(booking), any(), any());
//
//        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1", new UpdateReviewRequestDTO(7, "x")))
//                .isInstanceOf(TourValidationException.class);
    }

    @Test
    void updateReview_contentOnly_skipsAggregateUpdateAndReturnsMergedResponse() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 4, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));

        ReviewResponseDTO response = service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(null, "new content"));

        verify(reviewRepository).updateReview(eq("tour-1"), eq("booking-1"), eq(4),
                eq(null), eq("new content"), eq(NOW.toString()), eq(null));
        verify(reviewRepository, times(0)).getRatingAggregates("tour-1");
        assertThat(response.rate()).isEqualTo(4);
        assertThat(response.reviewContent()).isEqualTo("new content");
        assertThat(response.authorName()).isEqualTo("Jane Doe");
    }

    @Test
    void updateReview_rateUnchanged_takesContentOnlyPath() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 4, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));

        ReviewResponseDTO response = service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(4, "updated"));

        verify(reviewRepository).updateReview(eq("tour-1"), eq("booking-1"), eq(4),
                eq(null), eq("updated"), eq(NOW.toString()), eq(null));
        verify(reviewRepository, times(0)).getRatingAggregates("tour-1");
        assertThat(response.rate()).isEqualTo(4);
        assertThat(response.reviewContent()).isEqualTo("updated");
    }

    @Test
    void updateReview_rateChange_writesDualTableUpdateWithExpectedAggregates() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0));

        ReviewResponseDTO response = service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(5, "great"));

        ArgumentCaptor<RatingAggregateUpdate> aggregateCaptor = ArgumentCaptor.forClass(RatingAggregateUpdate.class);
        verify(reviewRepository).updateReview(eq("tour-1"), eq("booking-1"), eq(3),
                eq(5), eq("great"), eq(NOW.toString()), aggregateCaptor.capture());
        RatingAggregateUpdate update = aggregateCaptor.getValue();
        assertThat(update.expectedRatingSum()).isEqualTo(20L);
        assertThat(update.expectedReviews()).isEqualTo(5);
        assertThat(update.newRatingSum()).isEqualTo(22L);
        assertThat(update.newReviews()).isEqualTo(5);
        assertThat(update.newRating()).isEqualTo(round2(22.0 / 5));
        assertThat(response.rate()).isEqualTo(5);
        assertThat(response.reviewContent()).isEqualTo("great");
    }

    @Test
    void updateReview_retriesOnRateConditionFailureThenConflicts() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0));
        doThrow(new OptimisticLockException("review rate changed"))
                .when(reviewRepository).updateReview(any(), any(), eq(3), eq(5), any(), any(), any());

        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(5, "great")))
                .isInstanceOf(ReviewConflictException.class)
                .hasMessage("conflict, please retry");
        verify(reviewRepository, times(3)).updateReview(any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void updateReview_propagatesUnexpectedRepositoryException() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0));
        doThrow(new IllegalStateException("mongo down"))
                .when(reviewRepository).updateReview(any(), any(), anyInt(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(5, "great")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mongo down");
        verify(reviewRepository, times(1)).updateReview(any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void updateReview_contentOnlyPreLoopRatePinFailureFallsIntoRetryLoopAndSucceeds() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 4, "old");
        existing.setUserId("user-1");
        Review fresh = review("tour-1", "booking-1", "Jane Doe", 5, "old");
        fresh.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1"))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(fresh));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0));
        doThrow(new OptimisticLockException("rate drifted"))
                .doNothing()
                .when(reviewRepository).updateReview(any(), any(), anyInt(), any(), any(), any(), any());

        ReviewResponseDTO response = service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(null, "patched"));

        verify(reviewRepository, times(2)).updateReview(any(), any(), anyInt(), any(), any(), any(), any());
        assertThat(response.rate()).isEqualTo(5);
        assertThat(response.reviewContent()).isEqualTo("patched");
    }

    @Test
    void updateReview_throwsConflictWhenFreshReviewMissingDuringRetry() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1"))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(5, "great")))
                .isInstanceOf(ReviewConflictException.class)
                .hasMessage("review no longer exists");
    }

    @Test
    void updateReview_throwsConflictWhenReValidationFailsDuringRetry() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        Review fresh = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        fresh.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1"))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(fresh));

        try (MockedStatic<TourReviewValidator> validator = mockStatic(TourReviewValidator.class)) {
            validator.when(() -> TourReviewValidator.validateUpdateRequest(
                            eq("user-1"), eq(fresh), eq(booking), any(), any()))
                    .thenThrow(new TourValidationException(Map.of("booking", "must be STARTED or FINISHED")));

            assertThatThrownBy(() -> service.updateReview("booking-1", "user-1",
                    new UpdateReviewRequestDTO(5, "great")))
                    .isInstanceOf(ReviewConflictException.class)
                    .hasMessage("conflict, please retry");
        }

//        doThrow(new TourValidationException(java.util.Map.of("booking", "must be STARTED or FINISHED")))
//                .when(validator).validateUpdateRequest(eq("user-1"), eq(fresh), eq(booking), any(), any());

//        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1",
//                new UpdateReviewRequestDTO(5, "great")))
//                .isInstanceOf(ReviewConflictException.class)
//                .hasMessage("conflict, please retry");
    }

    @Test
    void updateReview_retriesOnAggregateOptimisticLockFailureThenSucceeds() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0))
                .thenReturn(new RatingAggregates(22L, 5, 4.4));

        doThrow(new OptimisticLockException("tour rating aggregate changed"))
                .doNothing().when(reviewRepository).updateReview(any(), any(), anyInt(), any(), any(), any(), any());

        ReviewResponseDTO response = service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(5, "great"));

        verify(reviewRepository, times(2)).updateReview(any(), any(), anyInt(), any(), any(), any(), any());
        verify(reviewRepository, times(2)).getRatingAggregates("tour-1");
        assertThat(response.rate()).isEqualTo(5);
    }

    @Test
    void updateReview_throwsConflictAfterMaxAggregateRetries() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0));

        doThrow(new OptimisticLockException("tour rating aggregate changed"))
                .when(reviewRepository).updateReview(any(), any(), anyInt(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(5, "great")))
                .isInstanceOf(ReviewConflictException.class)
                .hasMessage("conflict, please retry");
        verify(reviewRepository, times(3)).updateReview(any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void updateReview_rejectedUpdatedText_throwsAndDoesNotPersist() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 4, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        when(moderationService.moderate(any())).thenReturn(new ModerationResult(
                ModerationStatus.REJECTED, "contains a threat", List.of("THREAT"),
                "Please remove threatening language and resubmit."));

        assertThatThrownBy(() -> service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(null, "I will hurt you")))
                .isInstanceOf(ReviewModerationRejectedException.class)
                .hasMessage("contains a threat");

        verify(moderationService).moderate("I will hurt you");
        verify(reviewRepository, never()).updateReview(any(), any(), anyInt(), any(), any(), any(), any());
        verifyNoInteractions(eventPublisherService);
    }

    @Test
    void updateReview_rateOnly_doesNotInvokeModeration() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(20L, 5, 4.0));

        service.updateReview("booking-1", "user-1", new UpdateReviewRequestDTO(5, null));

        verify(moderationService, never()).moderate(any());
        verify(reviewRepository).updateReview(eq("tour-1"), eq("booking-1"), eq(3),
                eq(5), eq(null), eq(NOW.toString()), any());
    }

    @Test
    void updateReview_approvedUpdatedText_moderatesAndPersists() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 4, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));

        ReviewResponseDTO response = service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(null, "harsh but civil"));

        verify(moderationService).moderate("harsh but civil");
        verify(reviewRepository).updateReview(eq("tour-1"), eq("booking-1"), eq(4),
                eq(null), eq("harsh but civil"), eq(NOW.toString()), eq(null));
        assertThat(response.reviewContent()).isEqualTo("harsh but civil");
    }

    @Test
    void getReviewsByUserId_delegatesToRepository() {
        Review review = review("tour-1", "booking-1", "Jane", 5, "Great");
        when(reviewRepository.getReviewsByUserId("user-1")).thenReturn(List.of(review));

        List<Review> result = service.getReviewsByUserId("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReviewId()).isEqualTo("booking-1");
        verify(reviewRepository).getReviewsByUserId("user-1");
    }

    @Test
    void updateReview_rateChangeWithZeroAggregateReviews_computesZeroRating() {
        Booking booking = booking("booking-1", "user-1", "tour-1");
        Review existing = review("tour-1", "booking-1", "Jane Doe", 3, "old");
        existing.setUserId("user-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(reviewRepository.findById("tour-1", "booking-1")).thenReturn(Optional.of(existing));
        // Zero reviews → deriveRating(_, 0) → returns 0.0
        when(reviewRepository.getRatingAggregates("tour-1"))
                .thenReturn(new RatingAggregates(0L, 0, 0.0));

        ReviewResponseDTO response = service.updateReview("booking-1", "user-1",
                new UpdateReviewRequestDTO(5, "great"));

        ArgumentCaptor<RatingAggregateUpdate> aggregateCaptor = ArgumentCaptor.forClass(RatingAggregateUpdate.class);
        verify(reviewRepository).updateReview(eq("tour-1"), eq("booking-1"), eq(3),
                eq(5), eq("great"), eq(NOW.toString()), aggregateCaptor.capture());
        assertThat(aggregateCaptor.getValue().newRating()).isEqualTo(0.0);
        assertThat(response.rate()).isEqualTo(5);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Booking booking(String bookingId, String userId, String tourId) {
        Booking b = new Booking();
        b.setBookingId(bookingId);
        b.setUserId(userId);
        b.setTourId(tourId);
        b.setStartDate(LocalDate.parse("2026-05-01"));
        b.setDuration("10 days");
        b.setState(BookingStatus.FINISHED);
        b.setName("Tour Name");
        b.setDestination("Paris");
        b.setTravelAgentEmail("agent@example.com");
        b.setTravelAgent(new TravelAgentSnapshot("Agent Name", "agent@example.com", "+1", "messenger"));
        return b;
    }

    private static User user(String userId, String firstName, String lastName) {
        User u = new User();
        u.setId(userId);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        return u;
    }

    private static Review review(String tourId, String reviewId, String authorName, int rate, String content) {
        Review r = new Review();
        r.setTourId(tourId);
        r.setReviewId(reviewId);
        r.setAuthorName(authorName);
        r.setRate(rate);
        r.setReviewContent(content);
        r.setCreatedAt("2026-06-16");
        return r;
    }
}
