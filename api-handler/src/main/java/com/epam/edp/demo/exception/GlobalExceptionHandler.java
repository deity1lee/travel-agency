package com.epam.edp.demo.exception;

import com.epam.edp.demo.exception.booking.BookingAccessDeniedException;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.booking.BookingStateTransitionException;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.exception.document.DocumentValidationException;
import com.epam.edp.demo.exception.tour.TourNotFoundException;
import com.epam.edp.demo.exception.tour.TourValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<String> handleBookingNotFound(BookingNotFoundException ex) {
        log.warn("Returning 404 NOT FOUND: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
        log.warn("Returning 404 NOT FOUND: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(NoAvailableTravelAgentException.class)
    public ResponseEntity<String> handleBookingInvalidStateTransition(NoAvailableTravelAgentException ex) {
        log.warn("Returning 409 CONFLICT: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(BookingAccessDeniedException.class)
    public ResponseEntity<String> handleBookingAccessDenied(BookingAccessDeniedException ex) {
        log.warn("Returning 403 FORBIDDEN: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ex.getMessage());
    }

    @ExceptionHandler(BookingStateTransitionException.class)
    public ResponseEntity<String> handleBookingInvalidStateTransition(BookingStateTransitionException ex) {
        log.warn("Returning 409 CONFLICT: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(NoAvailableSpotsException.class)
    public ResponseEntity<String> handleTourNoAvailableSpotsException(NoAvailableSpotsException ex) {
        log.warn("Returning 409 CONFLICT: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(TourNotFoundException.class)
    public ResponseEntity<String> handleTourNotFound(TourNotFoundException ex) {
        log.warn("Returning 404 NOT FOUND: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(EventPublisherException.class)
    public void handleEventNotPublished(EventPublisherException ex) {
        log.warn("Unable to publish event change to SQS: {}", ex.getMessage());
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<String> handleBookingValidation(BookingValidationException ex) {
        log.warn("Returning 400 BAD REQUEST: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(DocumentValidationException.class)
    public ResponseEntity<String> handleDocumentValidation(DocumentValidationException ex) {
        log.warn("Returning 400 BAD REQUEST: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<String> handleUnauthenticated(UnauthenticatedException ex) {
        log.warn("Returning 401 UNAUTHORIZED: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Unauthorized");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception ex) {
        log.warn("Received error", ex.getCause());
        log.warn("Error stack trace: {}", (Object) ex.getStackTrace());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, List<String>> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                        .add(error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return response;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Map<String, String> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        }
        return errors;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Returning 400. Caught exception ", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Validation failed");

        Map<String, List<String>> errors = new HashMap<>();

        String errorMessage = ex.getMessage();
        if (errorMessage != null && errorMessage.contains("\"")) {
            String[] parts = errorMessage.split("\"");
            if (parts.length > 1) {
                String fieldName = parts[1];
                errors.put(fieldName, List.of("Invalid value provided"));
            }
        }

        if (errors.isEmpty()) {
            errors.put("request", List.of("Invalid request format"));
        }

        response.put("errors", errors);
        return response;
    }

    @ExceptionHandler(TourValidationException.class)
    public ResponseEntity<Map<String, Object>> handleTourValidation(TourValidationException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Validation failed", "errors", ex.getErrors()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return "User doesn't have a sufficient role to access the resource";
    }

    @ExceptionHandler(EmailSendingDisabledException.class)
    @ResponseStatus(HttpStatus.OK)
    public String handleAccessDenied(EmailSendingDisabledException ex) {
        log.warn("Email sending is disabled for testing");
        return ex.getMessage();
    }
}
