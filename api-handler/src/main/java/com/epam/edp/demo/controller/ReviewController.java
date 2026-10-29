package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.moderation.ModerationRejectionResponse;
import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.dto.review.ReviewListResponseDTO;
import com.epam.edp.demo.dto.review.ReviewResponseDTO;
import com.epam.edp.demo.dto.review.UpdateReviewRequestDTO;
import com.epam.edp.demo.dto.tour.TourReviewQueryDTO;
import com.epam.edp.demo.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "Reviews", description = "Browse tour reviews and submit reviews for completed bookings")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/tours/{tourId}/reviews")
    @Operation(summary = "List reviews for a tour")
    @ApiResponse(responseCode = "200", description = "Reviews page",
            content = @Content(schema = @Schema(implementation = ReviewListResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(responseCode = "404", description = "Reviews not found", content = @Content)
    public ReviewListResponseDTO getTourReviews(@Parameter(description = "Tour identifier")
                                                @PathVariable String tourId,
                                                @Valid @ModelAttribute TourReviewQueryDTO query) {
        log.info("Incoming request: GET /tours/{}/reviews", tourId);
        return reviewService.getTourReviews(tourId, query);
    }

    @PostMapping("/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a review for a started or finished booking")
    @ApiResponse(responseCode = "201", description = "Review created",
            content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(responseCode = "404", description = "Booking not found", content = @Content)
    @ApiResponse(responseCode = "422", description = "Review declined by moderation; action=EDIT_AND_RESUBMIT (fixable) or BLOCKED (terminal)",
            content = @Content(schema = @Schema(implementation = ModerationRejectionResponse.class)))
    public ReviewResponseDTO createReview(@CurrentUser UserContext user,
                                          @Parameter(description = "Booking identifier") @PathVariable String bookingId,
                                          @Valid @RequestBody CreateReviewRequestDTO request) {
        log.info("Incoming request: POST /bookings/{}/review", bookingId);
        return reviewService.createReview(bookingId, user.userId(), request);
    }

    @PutMapping("/bookings/{bookingId}/review")
    @Operation(summary = "Update an existing review for a booking")
    @ApiResponse(responseCode = "200", description = "Review updated",
            content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(responseCode = "404", description = "Review or booking not found", content = @Content)
    @ApiResponse(responseCode = "422", description = "Review declined by moderation; action=EDIT_AND_RESUBMIT (fixable) or BLOCKED (terminal)",
            content = @Content(schema = @Schema(implementation = ModerationRejectionResponse.class)))
    public ReviewResponseDTO updateReview(@CurrentUser UserContext user,
                                          @Parameter(description = "Booking identifier") @PathVariable String bookingId,
                                          @Valid @RequestBody UpdateReviewRequestDTO request) {
        log.info("Incoming request: PUT /bookings/{}/review", bookingId);
        return reviewService.updateReview(bookingId, user.userId(), request);
    }
}
