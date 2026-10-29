package com.epam.edp.demo.exception.auth;

import com.epam.edp.demo.controller.AuthController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler({EmailAlreadyExistsException.class, EditProfileException.class})
    public ResponseEntity<String> handleEmailExistsException(AuthValidationException ex) {
        log.warn("Returning 409 CONFLICT: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(AuthValidationException.class) // password
    public ResponseEntity<String> handleValidation(AuthValidationException ex) {
        String message = ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Auth validation error: " + message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Returning 401 UNAUTHORIZED: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidVerificationCode(InvalidVerificationCodeException ex) {
        log.warn("Returning 400 BAD_REQUEST for incorrect verification: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("attemptsRemaining", ex.getRemainingAttempts());

        return response;
    }

    @ExceptionHandler(VerificationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleVerificationFailed(VerificationFailedException ex) {
        log.warn("Returning 400 BAD_REQUEST for failed verification: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());

        return response;
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleIncorrectPasswordInAuthenticatedRequest(IncorrectPasswordException ex) {
        log.warn("Returning 403 FORBIDDEN for providing incorrect password: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleEmailNotVerified(EmailNotVerifiedException ex) {
        log.warn("Returning 403 FORBIDDEN due to email not being verified: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler(EmailSendException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleEmailSend(EmailSendException ex) {
        log.error("Returning 503 SERVICE_UNAVAILABLE for email send failure: {}", ex.getMessage());
        return "Unable to send verification email, please try again later";
    }

    @ExceptionHandler(CaptchaValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleCaptchaValidation(CaptchaValidationException ex) {
        log.warn("Returning 400 BAD_REQUEST for captcha validation failure: {}", ex.getMessage());
        return ex.getMessage();
    }
}
