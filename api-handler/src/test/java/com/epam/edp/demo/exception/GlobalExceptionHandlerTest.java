package com.epam.edp.demo.exception;

import com.epam.edp.demo.exception.booking.BookingAccessDeniedException;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.booking.BookingStateTransitionException;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.exception.tour.TourNotFoundException;
import com.epam.edp.demo.exception.tour.TourValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBookingNotFound_returns404WithMessageBody() {
        ResponseEntity<String> response = handler.handleBookingNotFound(
                new BookingNotFoundException("booking 123 missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("booking 123 missing");
    }

    @Test
    void handleUserNotFound_returns404WithMessageBody() {
        ResponseEntity<String> response = handler.handleUserNotFound(
                new UserNotFoundException("user 1 missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("user 1 missing");
    }

    @Test
    void handleNoAvailableTravelAgent_returns409Conflict() {
        ResponseEntity<String> response = handler.handleBookingInvalidStateTransition(
                new NoAvailableTravelAgentException("no agents"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("no agents");
    }

    @Test
    void handleBookingAccessDenied_returns403Forbidden() {
        ResponseEntity<String> response = handler.handleBookingAccessDenied(
                new BookingAccessDeniedException("not yours"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("not yours");
    }

    @Test
    void handleBookingStateTransition_returns409Conflict() {
        ResponseEntity<String> response = handler.handleBookingInvalidStateTransition(
                new BookingStateTransitionException("cannot transition"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("cannot transition");
    }

    @Test
    void handleTourNoAvailableSpots_returns409Conflict() {
        ResponseEntity<String> response = handler.handleTourNoAvailableSpotsException(
                new NoAvailableSpotsException("no seats"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("no seats");
    }

    @Test
    void handleTourNotFound_returns404WithMessageBody() {
        ResponseEntity<String> response = handler.handleTourNotFound(
                new TourNotFoundException("tour 7 missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("tour 7 missing");
    }

    @Test
    void handleEventNotPublished_isSilentVoid_andDoesNotThrow() {
        handler.handleEventNotPublished(new EventPublisherException("sqs down"));
        // No assertion: method returns void and is exercised for coverage.
    }

    @Test
    void handleBookingValidation_returns400BadRequest() {
        ResponseEntity<String> response = handler.handleBookingValidation(
                new BookingValidationException("bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("bad input");
    }

    @Test
    void handleGeneral_returns500WithPrefixedMessage() {
        ResponseEntity<String> response = handler.handleGeneral(new RuntimeException("kaboom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Internal server error: kaboom");
    }

    @Test
    void handleMethodArgumentNotValid_groupsErrorsByField() {
        BindingResult binding = mock(BindingResult.class);
        when(binding.getFieldErrors()).thenReturn(List.of(
                new FieldError("o", "email", "must not be blank"),
                new FieldError("o", "email", "must be a valid email"),
                new FieldError("o", "password", "must not be blank")));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class), binding);

        Map<String, Object> body = handler.handleValidation(ex);

        assertThat(body).containsEntry("message", "Validation failed");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) body.get("errors");
        assertThat(errors).containsOnlyKeys("email", "password");
        assertThat(errors.get("email")).containsExactlyInAnyOrder("must not be blank", "must be a valid email");
        assertThat(errors.get("password")).containsExactly("must not be blank");
    }

    @Test
    void handleConstraintViolationException_mapsPathToMessage() {
        ConstraintViolation<?> v1 = mock(ConstraintViolation.class);
        when(v1.getPropertyPath()).thenReturn(stubPath("user.email"));
        when(v1.getMessage()).thenReturn("must not be blank");
        ConstraintViolation<?> v2 = mock(ConstraintViolation.class);
        when(v2.getPropertyPath()).thenReturn(stubPath("user.age"));
        when(v2.getMessage()).thenReturn("must be positive");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v1, v2));

        Map<String, String> errors = handler.handleConstraintViolationException(ex);

        assertThat(errors)
                .containsEntry("user.email", "must not be blank")
                .containsEntry("user.age", "must be positive");
    }

    @Test
    void handleTourValidation_returns400WithErrorsMap() {
        Map<String, String> errors = Map.of("name", "must not be blank");
        ResponseEntity<Map<String, Object>> response = handler.handleTourValidation(
                new TourValidationException(errors));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("message", "Validation failed")
                .containsEntry("errors", errors);
    }

    private static Path stubPath(String value) {
        return new Path() {
            @Override
            public java.util.Iterator<Node> iterator() {
                return java.util.Collections.emptyIterator();
            }

            @Override
            public String toString() {
                return value;
            }
        };
    }
}
