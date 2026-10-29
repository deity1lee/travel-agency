package com.epam.edp.demo.repository.review;

public record RatingAggregateUpdate(
        long expectedRatingSum,
        int expectedReviews,
        long newRatingSum,
        int newReviews,
        double newRating
) {
}
