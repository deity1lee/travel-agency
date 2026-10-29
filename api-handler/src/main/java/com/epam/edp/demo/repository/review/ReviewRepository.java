package com.epam.edp.demo.repository.review;

import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.entity.tourModel.ReviewSortBy;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

    Optional<Review> findById(String tourId, String reviewId);

    List<Review> getReviewsByTourId(String tourId, int page, int pageSize, ReviewSortBy sortBy);

    List<Review> getReviewsByUserId(String userId);

    RatingAggregates getRatingAggregates(String tourId);

    int getReviewCountForTour(String tourId);

    void createReview(Review review, RatingAggregateUpdate aggregateUpdate);

    void saveAll(List<Review> reviews);

    void updateReview(String tourId, String reviewId, int expectedRate,
                      Integer newRate, String newReviewContent, String updatedAt,
                      RatingAggregateUpdate aggregateUpdate);
}
