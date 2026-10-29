package com.epam.edp.demo.dto.tour;

import com.epam.edp.demo.entity.tourModel.ReviewSortBy;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record TourReviewQueryDTO(
        @Parameter(description = "Page number (1-based)", schema = @Schema(type = "integer", defaultValue = "1", minimum = "1"))
        @Min(value = 1, message = "must be >= 1")
        Integer page,

        @Parameter(description = "Items per page", schema = @Schema(type = "integer", defaultValue = "4", minimum = "1"))
        @Min(value = 1, message = "must be >= 1")
        Integer pageSize,

        @Parameter(description = "Sort order", schema = @Schema(implementation = ReviewSortBy.class))
        ReviewSortBy sortBy
) {
    public TourReviewQueryDTO {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 4 : pageSize;
        sortBy = sortBy == null ? ReviewSortBy.RATING_DESC : sortBy;
    }
}
