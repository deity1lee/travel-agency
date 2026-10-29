package com.epam.edp.demo.service.review;

import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.dto.review.ReviewListResponseDTO;
import com.epam.edp.demo.dto.review.ReviewResponseDTO;
import com.epam.edp.demo.dto.review.UpdateReviewRequestDTO;
import com.epam.edp.demo.dto.tour.TourReviewQueryDTO;
import com.epam.edp.demo.entity.review.Review;

import java.util.List;

public interface ReviewService {

    ReviewListResponseDTO getTourReviews(String tourId, TourReviewQueryDTO tourReviewQueryDTO);

    ReviewResponseDTO createReview(String bookingId, String callerUserId, CreateReviewRequestDTO body);

    ReviewResponseDTO updateReview(String bookingId, String callerUserId, UpdateReviewRequestDTO body);

    List<Review> getReviewsByUserId(String userId);
}
