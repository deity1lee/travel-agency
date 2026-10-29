package com.epam.edp.demo.repository.review;

import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.entity.tourModel.ReviewSortBy;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.exception.review.DuplicateReviewException;
import com.epam.edp.demo.exception.review.OptimisticLockException;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class ReviewRepositoryImpl implements ReviewRepository {

    private final MongoTemplate mongoTemplate;
    private final TransactionTemplate transactionTemplate;

    public ReviewRepositoryImpl(MongoTemplate mongoTemplate,
                                TransactionTemplate transactionTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Optional<Review> findById(String tourId, String reviewId) {
        Query query = new Query(Criteria.where("_id").is(reviewId).and("tourId").is(tourId));
        return Optional.ofNullable(mongoTemplate.findOne(query, Review.class));
    }

    @Override
    public List<Review> getReviewsByTourId(String tourId, int page, int pageSize, ReviewSortBy reviewSortBy) {
        log.info("Fetching reviews for tour by id {}", tourId);

        boolean byDate = reviewSortBy == ReviewSortBy.NEWEST || reviewSortBy == ReviewSortBy.OLDEST;
        String sortField = byDate ? "createdAt" : "rate";
        boolean isAscending = reviewSortBy == ReviewSortBy.RATING_ASC || reviewSortBy == ReviewSortBy.OLDEST;
        Sort.Direction direction = isAscending ? Sort.Direction.ASC : Sort.Direction.DESC;

        int skip = Math.max(0, (page - 1) * pageSize);

        Sort sort = Sort.by(direction, sortField)
                .and(Sort.by(Sort.Direction.ASC, "_id"));
        Query query = new Query(Criteria.where("tourId").is(tourId))
                .with(sort)
                .skip(skip)
                .limit(pageSize);

        List<Review> reviews = mongoTemplate.find(query, Review.class);
        log.info("Returning {} reviews for tour {}", reviews.size(), tourId);
        return reviews;
    }

    @Override
    public List<Review> getReviewsByUserId(String userId) {
        log.info("Fetching reviews for user {}", userId);
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, Review.class);
    }

    @Override
    public RatingAggregates getRatingAggregates(String tourId) {
        Tour tour = readTourAggregates(tourId);
        if (tour == null) {
            return new RatingAggregates(0L, 0, 0.0);
        }
        long ratingSum = tour.getRatingSum() == null ? 0L : tour.getRatingSum();
        int reviews = tour.getReviews() == null ? 0 : tour.getReviews();
        double rating = tour.getRating() == null ? 0.0 : tour.getRating();
        return new RatingAggregates(ratingSum, reviews, rating);
    }

    @Override
    public int getReviewCountForTour(String tourId) {
        Tour tour = readTourAggregates(tourId);
        if (tour == null || tour.getReviews() == null) {
            return 0;
        }
        return tour.getReviews();
    }

    @Override
    public void createReview(Review review, RatingAggregateUpdate aggregateUpdate) {
        transactionTemplate.executeWithoutResult(status -> {
            insertReview(review);
            applyTourAggregate(review.getTourId(), aggregateUpdate);
        });
    }

    @Override
    public void saveAll(List<Review> reviews) {
        if (reviews.isEmpty()) {
            log.info("No reviews to save");
            return;
        }
        log.info("Saving {} reviews in batch", reviews.size());
        mongoTemplate.insert(reviews, Review.class);
        log.info("Saved {} reviews", reviews.size());
    }

    @Override
    public void updateReview(String tourId, String reviewId, int expectedRate,
                             Integer newRate, String newReviewContent, String updatedAt,
                             RatingAggregateUpdate aggregateUpdate) {
        if (aggregateUpdate == null) {
            updateReviewOnly(tourId, reviewId, expectedRate, newReviewContent, updatedAt);
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            updateReviewWithRate(tourId, reviewId, expectedRate, newRate, newReviewContent, updatedAt);
            applyTourAggregate(tourId, aggregateUpdate);
        });
    }

    private Tour readTourAggregates(String tourId) {
        Query query = new Query(Criteria.where("_id").is(tourId));
        query.fields().include("ratingSum").include("reviews").include("rating");
        return mongoTemplate.findOne(query, Tour.class);
    }

    private void insertReview(Review review) {
        try {
            mongoTemplate.insert(review);
        } catch (DuplicateKeyException e) {
            throw new DuplicateReviewException(
                    "review already exists for booking " + review.getReviewId(), e);
        }
    }

    private void updateReviewOnly(String tourId, String reviewId,
                                  int expectedRate, String newReviewContent, String updatedAt) {
        Query query = new Query(Criteria.where("_id").is(reviewId)
                .and("tourId").is(tourId)
                .and("rate").is(expectedRate));
        Update update = new Update().set("updatedAt", updatedAt);
        applyContentChange(update, newReviewContent);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Review.class);
        if (result.getMatchedCount() == 0) {
            throw new OptimisticLockException(
                    "review rate changed before content update for review " + reviewId);
        }
    }

    private void updateReviewWithRate(String tourId, String reviewId, int expectedRate,
                                      int newRate, String newReviewContent, String updatedAt) {
        Query query = new Query(Criteria.where("_id").is(reviewId)
                .and("tourId").is(tourId)
                .and("rate").is(expectedRate));
        Update update = new Update()
                .set("rate", newRate)
                .set("updatedAt", updatedAt);
        applyContentChange(update, newReviewContent);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Review.class);
        if (result.getMatchedCount() == 0) {
            throw new OptimisticLockException(
                    "review rate changed before rate update for review " + reviewId);
        }
    }

    private static void applyContentChange(Update update, String newReviewContent) {
        if (newReviewContent != null) {
            if (newReviewContent.isBlank()) {
                update.unset("reviewContent");
            } else {
                update.set("reviewContent", newReviewContent);
            }
        }
    }

    private void applyTourAggregate(String tourId, RatingAggregateUpdate aggregateUpdate) {
        Query query = new Query(Criteria.where("_id").is(tourId)
                .and("ratingSum").is(aggregateUpdate.expectedRatingSum())
                .and("reviews").is(aggregateUpdate.expectedReviews()));
        Update update = new Update()
                .set("ratingSum", aggregateUpdate.newRatingSum())
                .set("reviews", aggregateUpdate.newReviews())
                .set("rating", aggregateUpdate.newRating());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Tour.class);
        if (result.getMatchedCount() == 0) {
            throw new OptimisticLockException(
                    "tour rating aggregate changed for tour " + tourId);
        }
    }
}
