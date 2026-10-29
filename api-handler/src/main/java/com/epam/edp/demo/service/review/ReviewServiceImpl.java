package com.epam.edp.demo.service.review;

import com.epam.edp.demo.dto.moderation.ModerationResult;
import com.epam.edp.demo.dto.reports.FeedbackEvent;
import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.dto.review.ReviewListResponseDTO;
import com.epam.edp.demo.dto.review.ReviewResponseDTO;
import com.epam.edp.demo.dto.review.UpdateReviewRequestDTO;
import com.epam.edp.demo.dto.tour.TourReviewQueryDTO;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.review.ReviewConflictException;
import com.epam.edp.demo.exception.review.ReviewModerationRejectedException;
import com.epam.edp.demo.exception.review.ReviewNotFoundException;
import com.epam.edp.demo.exception.tour.TourValidationException;
import com.epam.edp.demo.mapper.ReviewMapper;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.exception.review.DuplicateReviewException;
import com.epam.edp.demo.exception.review.OptimisticLockException;
import com.epam.edp.demo.repository.review.RatingAggregateUpdate;
import com.epam.edp.demo.repository.review.RatingAggregates;
import com.epam.edp.demo.repository.review.ReviewRepository;
import com.epam.edp.demo.service.document.S3PresignedUrlService;
import com.epam.edp.demo.service.moderation.ModerationService;
import com.epam.edp.demo.service.reports.EventMapper;
import com.epam.edp.demo.service.reports.EventPublisherService;
import com.epam.edp.demo.util.JitteredBackoff;
import com.epam.edp.demo.validation.review.TourReviewValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_AGGREGATE_RETRY_ATTEMPTS = 3;

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final JitteredBackoff backoff;
    private final EventPublisherService eventPublisherService;
    private final ModerationService moderationService;
    private final S3PresignedUrlService presignedUrlService;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             BookingRepository bookingRepository,
                             UserRepository userRepository,
                             Clock clock,
                             JitteredBackoff backoff,
                             EventPublisherService eventPublisherService,
                             ModerationService moderationService,
                             S3PresignedUrlService presignedUrlService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.backoff = backoff;
        this.eventPublisherService = eventPublisherService;
        this.moderationService = moderationService;
        this.presignedUrlService = presignedUrlService;
    }

    @Override
    public ReviewListResponseDTO getTourReviews(String tourId, TourReviewQueryDTO tourReviewQueryDTO) {
        int page = tourReviewQueryDTO.page();
        int pageSize = tourReviewQueryDTO.pageSize();
        log.info("Fetching tour reviews for tour {}, page={}, pageSize={}, sortBy={}",
                tourId, page, pageSize, tourReviewQueryDTO.sortBy().name());
        List<Review> reviews = reviewRepository.getReviewsByTourId(tourId,
                page,
                pageSize,
                tourReviewQueryDTO.sortBy());

        String bucketName = presignedUrlService.getDocumentsBucketName();
        reviews.stream()
                .filter(r -> r.getAuthorImageUrl() == null)
                .forEach(r -> {
                    String s3Key = String.format(S3PresignedUrlService.PICTURE_UPLOAD_URL_BASE, r.getUserId());
                    if (presignedUrlService.verifyFileExistsByKey(bucketName, s3Key)) {
                        String retrieveUrl = presignedUrlService.generateRetrieveUrl(bucketName, s3Key);
                        r.setAuthorImageUrl(retrieveUrl);
                    }
                });

        int reviewCount = reviewRepository.getReviewCountForTour(tourId);
        log.info("Review count for tour {}, is {}", tourId, reviewCount);
        return ReviewMapper.toReviewListResponse(reviews, page, pageSize, reviewCount);
    }

    @Override
    public ReviewResponseDTO createReview(String bookingId, String callerUserId, CreateReviewRequestDTO body) {
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("booking not found"));

        String tourId = booking.getTourId();
        TourReviewValidator.validateCreateRequest(callerUserId, booking, body, LocalDate.now(clock));

        reviewRepository.findById(tourId, bookingId).ifPresent(existing -> {
            throw new ReviewConflictException("A review already exists for this booking. You can only update it.");
        });

        moderateOrReject(body.reviewContent());

        User profile = userRepository.findById(callerUserId)
                .orElseThrow(() -> new BookingNotFoundException("user not found"));

        String authorName = profile.getFirstName() + " " + profile.getLastName();
        String now = LocalDate.now(clock).toString();

        Review review = new Review();
        review.setTourId(tourId);
        review.setReviewId(bookingId);
        review.setUserId(callerUserId);
        review.setAuthorName(authorName.trim());
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        review.setRate(body.rate());
        review.setReviewContent(body.reviewContent());

        for (int attempt = 1; attempt <= MAX_AGGREGATE_RETRY_ATTEMPTS; attempt++) {
            RatingAggregates current = reviewRepository.getRatingAggregates(tourId);
            long newRatingSum = current.ratingSum() + body.rate();
            int newReviews = current.reviews() + 1;
            double newRating = deriveRating(newRatingSum, newReviews);
            RatingAggregateUpdate aggregateUpdate = new RatingAggregateUpdate(
                    current.ratingSum(), current.reviews(),
                    newRatingSum, newReviews, newRating);
            try {
                reviewRepository.createReview(review, aggregateUpdate);
                sendReviewEvent(review, body.rate(), null, booking);
                return ReviewMapper.toReviewResponse(review);
            } catch (DuplicateReviewException e) {
                throw new ReviewConflictException("A review already exists for this booking. You can only update it.");
            } catch (OptimisticLockException e) {
                log.info("Tour aggregate optimistic-lock failed for tour {} on attempt {}, retrying", tourId, attempt, e);
                if (attempt < MAX_AGGREGATE_RETRY_ATTEMPTS) {
                    backoff.sleep();
                }
            }
        }
        throw new ReviewConflictException("conflict, please retry");
    }

    @Override
    public ReviewResponseDTO updateReview(String bookingId, String callerUserId, UpdateReviewRequestDTO body) {
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("booking not found"));
        String tourId = booking.getTourId();
        String reviewId = bookingId;
        Review existing = reviewRepository.findById(tourId, reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("review not found"));
        LocalDate today = LocalDate.now(clock);
        TourReviewValidator.validateUpdateRequest(callerUserId, existing, booking, body, today);
        int oldRate = existing.getRate();
        int newRate = body.rate() != null ? body.rate() : oldRate;
        String content = body.reviewContent();
        moderateOrReject(content);
        if (newRate == oldRate) {
            ReviewResponseDTO immediate = tryContentOnlyUpdate(
                    tourId, reviewId, oldRate, existing, newRate, content, today.toString());
            if (immediate != null) {
                return immediate;
            }
        } else {
            sendReviewEvent(existing, newRate, oldRate, booking);
        }
        return retryUpdateWithAggregates(tourId, reviewId, callerUserId, booking, body, today, today.toString(), content);
    }

    private ReviewResponseDTO retryUpdateWithAggregates(String tourId, String reviewId, String callerUserId,
                                                        Booking booking, UpdateReviewRequestDTO body,
                                                        LocalDate today, String now, String content) {
        for (int attempt = 1; attempt <= MAX_AGGREGATE_RETRY_ATTEMPTS; attempt++) {
            Review fresh = reviewRepository.findById(tourId, reviewId)
                    .orElseThrow(() -> new ReviewConflictException("review no longer exists"));
            revalidateForRetry(callerUserId, fresh, booking, body, today);
            int freshOldRate = fresh.getRate();
            int freshNewRate = body.rate() != null ? body.rate() : freshOldRate;
            ReviewResponseDTO result;
            if (freshNewRate == freshOldRate) {
                result = tryContentOnlyUpdate(
                        tourId, reviewId, freshOldRate, fresh, freshNewRate, content, now);
            } else {
                result = tryFullUpdate(
                        tourId, reviewId, fresh, freshOldRate, freshNewRate, content, now, attempt);
            }
            if (result != null) {
                return result;
            }
            if (attempt < MAX_AGGREGATE_RETRY_ATTEMPTS) {
                backoff.sleep();
            }
        }
        throw new ReviewConflictException("conflict, please retry");
    }

    private ReviewResponseDTO tryContentOnlyUpdate(String tourId, String reviewId, int pinnedRate,
                                                   Review reviewToReturn, int returnRate,
                                                   String content, String now) {
        try {
            reviewRepository.updateReview(tourId, reviewId, pinnedRate, null, content, now, null);
            return ReviewMapper.toUpdateResponse(reviewToReturn, returnRate, content);
        } catch (OptimisticLockException e) {
            log.info("Rate-pin update failed for tour {} review {}", tourId, reviewId, e);
            return null;
        }
    }

    private ReviewResponseDTO tryFullUpdate(String tourId, String reviewId, Review fresh,
                                            int freshOldRate, int freshNewRate,
                                            String content, String now, int attempt) {
        RatingAggregates current = reviewRepository.getRatingAggregates(tourId);
        long newRatingSum = current.ratingSum() + (freshNewRate - freshOldRate);
        int reviewsCount = current.reviews();
        double newRating = deriveRating(newRatingSum, reviewsCount);
        RatingAggregateUpdate aggregateUpdate = new RatingAggregateUpdate(
                current.ratingSum(), current.reviews(),
                newRatingSum, reviewsCount, newRating);
        try {
            reviewRepository.updateReview(tourId, reviewId, freshOldRate, freshNewRate,
                    content, now, aggregateUpdate);
            return ReviewMapper.toUpdateResponse(fresh, freshNewRate, content);
        } catch (OptimisticLockException e) {
            log.info("Conditional check failed on update for tour {} on attempt {}, retrying",
                    tourId, attempt, e);
            return null;
        }
    }

    private void revalidateForRetry(String callerUserId, Review fresh, Booking booking,
                                    UpdateReviewRequestDTO body, LocalDate today) {
        try {
            TourReviewValidator.validateUpdateRequest(callerUserId, fresh, booking, body, today);
        } catch (TourValidationException e) {
            throw new ReviewConflictException("conflict, please retry", e);
        }
    }

    @Override
    public List<Review> getReviewsByUserId(String userId) {
        log.info("Getting reviews by user id {}", userId);
        return reviewRepository.getReviewsByUserId(userId);
    }

    private void moderateOrReject(String reviewContent) {
        if (reviewContent == null || reviewContent.isBlank()) {
            return;
        }
        ModerationResult verdict = moderationService.moderate(reviewContent);
        if (verdict.isApproved()) {
            return;
        }
        log.info("Review rejected by moderation: status={} categories={}",
                verdict.status(), verdict.categories());
        throw new ReviewModerationRejectedException(verdict);
    }

    public void sendReviewEvent(Review review, Integer newRate, Integer oldRate, Booking booking) {
        log.info("sending review event for review={}", review.getReviewId());
        FeedbackEvent event = EventMapper.toFeedbackEvent(review, newRate, oldRate, booking);

        log.info("publishing SQSEvent: {}", event);
        eventPublisherService.publish(event);
    }

    private static double deriveRating(long ratingSum, int reviews) {
        if (reviews == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) ratingSum / reviews)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
