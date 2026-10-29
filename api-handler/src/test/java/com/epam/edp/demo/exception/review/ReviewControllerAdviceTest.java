package com.epam.edp.demo.exception.review;

import com.epam.edp.demo.dto.moderation.ModerationRejectionResponse;
import com.epam.edp.demo.dto.moderation.ModerationResult;
import com.epam.edp.demo.dto.moderation.ModerationStatus;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.tour.TourValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewControllerAdviceTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private ReviewControllerAdvice advice;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = mock(GlobalExceptionHandler.class);
        advice = new ReviewControllerAdvice(globalExceptionHandler);
    }

    @Test
    void handleReviewNotFound_returns404WithMessageBody() {
        ResponseEntity<String> response = advice.handleReviewNotFound(
                new ReviewNotFoundException("review missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("review missing");
    }

    @Test
    void handleReviewConflict_returns409WithMessageBody() {
        ResponseEntity<String> response = advice.handleReviewConflict(
                new ReviewConflictException("already reviewed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("already reviewed");
    }

    @Test
    void handleBookingNotFound_returns404WithMessageBody() {
        ResponseEntity<String> response = advice.handleBookingNotFound(
                new BookingNotFoundException("booking missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("booking missing");
    }

    @Test
    void handleModerationRejected_needsEdit_returns422WithEditAndResubmitAction() {
        ModerationResult verdict = new ModerationResult(
                ModerationStatus.NEEDS_EDIT, "mild profanity", List.of("PROFANITY"),
                "Please remove the profanity and resubmit.");

        ResponseEntity<ModerationRejectionResponse> response =
                advice.handleModerationRejected(new ReviewModerationRejectedException(verdict));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        ModerationRejectionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("NEEDS_EDIT");
        assertThat(body.action()).isEqualTo("EDIT_AND_RESUBMIT");
        assertThat(body.reason()).isEqualTo("mild profanity");
        assertThat(body.categories()).containsExactly("PROFANITY");
        assertThat(body.userMessage()).isEqualTo("Please remove the profanity and resubmit.");
    }

    @Test
    void handleModerationRejected_rejected_returns422WithBlockedAction() {
        ModerationResult verdict = new ModerationResult(
                ModerationStatus.REJECTED, "contains a threat", List.of("THREAT"),
                "This review cannot be published.");

        ResponseEntity<ModerationRejectionResponse> response =
                advice.handleModerationRejected(new ReviewModerationRejectedException(verdict));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        ModerationRejectionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("REJECTED");
        assertThat(body.action()).isEqualTo("BLOCKED");
        assertThat(body.userMessage()).isEqualTo("This review cannot be published.");
    }

    @Test
    void handleModerationRejected_blankUserMessage_fallsBackToDefaultPerAction() {
        ModerationResult needsEdit = new ModerationResult(
                ModerationStatus.NEEDS_EDIT, "off-topic", List.of("OFF_TOPIC"), "");
        ModerationResult rejected = new ModerationResult(
                ModerationStatus.REJECTED, "hate speech", List.of("HATE_SPEECH"), "");

        ModerationRejectionResponse editBody =
                advice.handleModerationRejected(new ReviewModerationRejectedException(needsEdit)).getBody();
        ModerationRejectionResponse blockedBody =
                advice.handleModerationRejected(new ReviewModerationRejectedException(rejected)).getBody();

        assertThat(editBody).isNotNull();
        assertThat(editBody.userMessage()).isEqualTo("Please revise your review and resubmit.");
        assertThat(blockedBody).isNotNull();
        assertThat(blockedBody.userMessage()).isEqualTo("This review can't be published.");
    }

    @SuppressWarnings("unused")
    public static class Stub {
        public void getTourReviews() {}
        public void someOtherMethod() {}
        public void anyMethod() {}
    }
}
