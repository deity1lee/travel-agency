package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.review.ReviewListResponseDTO;
import com.epam.edp.demo.dto.review.ReviewResponseDTO;
import com.epam.edp.demo.entity.review.Review;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewListResponseDTO toReviewListResponse(List<Review> reviews, int page, int pageSize, int totalItems) {
        int totalPages = pageSize == 0 ? 0 : (totalItems + pageSize - 1) / pageSize;
        return new ReviewListResponseDTO(
                toReviewResponseList(reviews),
                page,
                pageSize,
                totalPages,
                totalItems);
    }

    public static ReviewResponseDTO toReviewResponse(Review review) {
        return new ReviewResponseDTO(
                review.getAuthorName(),
                review.getAuthorImageUrl(),
                review.getCreatedAt(),
                review.getRate(),
                review.getReviewContent()
        );
    }

    public static ReviewResponseDTO toUpdateResponse(Review existing, int newRate, String bodyContent) {
        String responseContent;
        if (bodyContent == null) {
            responseContent = existing.getReviewContent();
        } else if (bodyContent.isBlank()) {
            responseContent = null;
        } else {
            responseContent = bodyContent;
        }
        return new ReviewResponseDTO(
                existing.getAuthorName(),
                existing.getAuthorImageUrl(),
                existing.getCreatedAt(),
                newRate,
                responseContent);
    }

    private static List<ReviewResponseDTO> toReviewResponseList(List<Review> reviews) {
        return reviews.stream()
                .map(ReviewMapper::toReviewResponse)
                .collect(Collectors.toList());
    }
}
