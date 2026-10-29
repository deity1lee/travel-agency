package com.epam.edp.demo.exception.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void handleEmailExists_returns409WithMessageBody() {
        ResponseEntity<String> response = handler.handleEmailExistsException(
                new EmailAlreadyExistsException("email taken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("email taken");
    }

    @Test
    void handleAuthValidation_returns400WithPrefixedMessage() {
        ResponseEntity<String> response = handler.handleValidation(
                new AuthValidationException("password too weak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Auth validation error: password too weak");
    }

    @Test
    void handleBadCredentials_returns401Unauthorized() {
        ResponseEntity<String> response = handler.handleBadCredentials(
                new BadCredentialsException("bad creds"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("bad creds");
    }

    @Test
    void handleInvalidVerificationCode_returns400WithInvalidCopy() {
        Map<String, Object> response = handler.handleInvalidVerificationCode(
                new InvalidVerificationCodeException("Invalid verification code", 4));

        assertThat(response.get("message")).isEqualTo("Invalid verification code");
    }

    @Test
    void handleEmailSend_returns500WithGenericCopyNotProviderDetail() {
        String response = handler.handleEmailSend(
                new EmailSendException("SendGrid 403 verified sender detail"));

        assertThat(response).isEqualTo("Unable to send verification email, please try again later");
    }

    @Test
    void handleCaptchaValidation_returns400WithExceptionMessage() {
        String response = handler.handleCaptchaValidation(
                new CaptchaValidationException("Captcha verification failed"));

        assertThat(response).isEqualTo("Captcha verification failed");
    }

    @Test
    void handleCaptchaValidation_scoreTooLow_returns400WithScoreMessage() {
        String response = handler.handleCaptchaValidation(
                new CaptchaValidationException("Captcha score too low, request rejected"));

        assertThat(response).isEqualTo("Captcha score too low, request rejected");
    }
}
