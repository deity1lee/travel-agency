package com.epam.edp.demo.dto.review;

import org.hibernate.validator.constraints.Range;

import com.epam.edp.demo.validation.review.ValidReviewContentPresence;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@ValidReviewContentPresence
public record CreateReviewRequestDTO(
        @Schema(description = "Rating to give a tour", example = "5")
        @Range(min = 1, max = 5, message = "must be between 1 and 5")
        @NotNull(message = "Rating must be provided")
        Integer rate,

        @Schema(description = "Contents of the review", example = "Great experience")
        String reviewContent
) {
}
