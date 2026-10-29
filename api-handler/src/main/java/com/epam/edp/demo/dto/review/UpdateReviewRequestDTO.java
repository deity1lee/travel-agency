package com.epam.edp.demo.dto.review;

import org.hibernate.validator.constraints.Range;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateReviewRequestDTO(
        @Schema(description = "Rating to update for a tour", example = "5")
        @Range(min = 1, max = 5, message = "must be between 1 and 5")
        Integer rate,

        @Schema(description = "Contents of the review", example = "Great experience")
        String reviewContent
) {
}
