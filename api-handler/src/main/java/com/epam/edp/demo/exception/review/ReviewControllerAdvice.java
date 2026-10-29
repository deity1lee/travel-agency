package com.epam.edp.demo.exception.review;

import com.epam.edp.demo.controller.ReviewController;
import com.epam.edp.demo.dto.moderation.ModerationRejectionResponse;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@RestControllerAdvice(assignableTypes = ReviewController.class)
public class ReviewControllerAdvice {

    private final GlobalExceptionHandler globalExceptionHandler;

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<String> handleReviewNotFound(ReviewNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(ReviewConflictException.class)
    public ResponseEntity<String> handleReviewConflict(ReviewConflictException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<String> handleBookingNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(ReviewModerationRejectedException.class)
    public ResponseEntity<ModerationRejectionResponse> handleModerationRejected(
            ReviewModerationRejectedException ex) {
        String action = ModerationFeedback.action(ex.status());
        String userMessage = ex.userMessage() == null || ex.userMessage().isBlank()
                ? ModerationFeedback.defaultMessage(ex.status())
                : ex.userMessage();
        log.info("Returning 422 from moderation: status={} action={} categories={}",
                ex.status(), action, ex.categories());
        ModerationRejectionResponse body = new ModerationRejectionResponse(
                ex.status().name(), action, ex.reason(), ex.categories(), userMessage);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
}